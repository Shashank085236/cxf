/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.attachment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;

public class AttachmentDeserializer {

    public static final String ATTACHMENT_DIRECTORY = "attachment-directory";

    public static final String ATTACHMENT_MEMORY_THRESHOLD = "attachment-memory-threshold";

    public static final int THRESHOLD = 1024 * 100; //100K (byte unit)

    private static final Pattern CONTENT_TYPE_BOUNDARY_PATTERN = Pattern.compile("boundary=\"?([^\";]*)");

    // TODO: Is there a better way to detect boundaries in the message content?
    // It seems constricting to assume the boundary will start with ----=_Part_
    private static final Pattern INPUT_STREAM_BOUNDARY_PATTERN =
            Pattern.compile("^--(----=_Part_\\S*)", Pattern.MULTILINE);

    private boolean lazyLoading = true;

    private PushbackInputStream stream;

    private byte boundary[];

    private String contentType;

    private LazyAttachmentCollection attachments;

    private Message message;

    private InputStream body;
    
    private Set<DelegatingInputStream> loaded = new HashSet<DelegatingInputStream>();

    public AttachmentDeserializer(Message message) {
        this.message = message;
    }

    public void initializeAttachments() throws IOException {
        initializeRootMessage();

        attachments = new LazyAttachmentCollection(this);
        message.setAttachments(attachments);
    }

    protected void initializeRootMessage() throws IOException {
        contentType = (String) message.get(Message.CONTENT_TYPE);

        if (contentType == null) {
            throw new IllegalStateException("Content-Type can not be empty!");
        }

        if (message.getContent(InputStream.class) == null) {
            throw new IllegalStateException("An InputStream must be provided!");
        }

        if (contentType.toLowerCase().indexOf("multipart/related") != -1) {
            String boundaryString = findBoundaryFromContentType(contentType);
            if (null == boundaryString) {                
                boundaryString = findBoundaryFromInputStream();
            }
            // If a boundary still wasn't found, throw an exception
            if (null == boundaryString) {
                throw new IOException("Couldn't determine the boundary from the message!");
            }
            boundary = boundaryString.getBytes();

            stream = new PushbackInputStream(message.getContent(InputStream.class),
                    boundary.length * 2);
            if (!readTillFirstBoundary(stream, boundary)) {
                throw new IOException("Couldn't find MIME boundary: " + new String(boundary));
            }

            try {
                // TODO: Do we need to copy these headers somewhere?
                new InternetHeaders(stream);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }

            body = new DelegatingInputStream(new MimeBodyPartInputStream(stream, boundary));
            message.setContent(InputStream.class, body);
        }
    }

    private String findBoundaryFromContentType(String ct) throws IOException {
        // Use regex to get the boundary and return null if it's not found
        Matcher m = CONTENT_TYPE_BOUNDARY_PATTERN.matcher(ct);
        return m.find() ? "--" + m.group(1) : null;
    }

    private String findBoundaryFromInputStream() throws IOException {
        InputStream is = message.getContent(InputStream.class);
        //boundary should definitely be in the first 2K;
        PushbackInputStream in = new PushbackInputStream(is, 4096);
        byte buf[] = new byte[2048];
        int i = in.read(buf);
        String msg = new String(buf, 0, i);
        in.unread(buf, 0, i);
        
        // Reset the input stream since we'll need it again later
        message.setContent(InputStream.class, in);

        // Use regex to get the boundary and return null if it's not found
        Matcher m = INPUT_STREAM_BOUNDARY_PATTERN.matcher(msg);
        return m.find() ? "--" + m.group(1) : null;
    }
    
    private void setStreamedAttachmentProperties(CachedOutputStream bos) throws IOException {
        Object directory = message.getContextualProperty(ATTACHMENT_DIRECTORY);
        if (directory != null) {
            if (directory instanceof File) {
                bos.setOutputDir((File)directory);
            } else {
                bos.setOutputDir(new File((String)directory));
            }
        }
        
        Object threshold = message.getContextualProperty(ATTACHMENT_MEMORY_THRESHOLD);
        if (threshold != null) {
            if (threshold instanceof Long) {
                bos.setThreshold((Long)threshold);
            } else {
                bos.setThreshold(Long.valueOf((String)threshold));
            }
        } else {
            bos.setThreshold(THRESHOLD);
        }
    }

    public AttachmentImpl readNext() throws IOException {
        // Cache any mime parts that are currently being streamed
        cacheStreamedAttachments();

        int v = stream.read();
        if (v == -1) {
            return null;
        }
        stream.unread(v);


        InternetHeaders headers;
        try {
            headers = new InternetHeaders(stream);
        } catch (MessagingException e) {
            // TODO create custom IOException
            throw new RuntimeException(e);
        }

        String id = headers.getHeader("Content-ID", null);
        if (id != null && id.startsWith("<")) {
            id = id.substring(1, id.length() - 1);
        } else {
            //no Content-ID, set cxf default ID
            id = "Content-ID: <root.message@cxf.apache.org";
        }

        id = URLDecoder.decode(id.startsWith("cid:") ? id.substring(4) : id, "UTF-8");

        AttachmentImpl att = new AttachmentImpl(id);
        setupAttachment(att, headers);
        return att;
    }

    private void cacheStreamedAttachments() throws IOException {
        if (body instanceof DelegatingInputStream
            && !((DelegatingInputStream) body).isClosed()) {

            cache((DelegatingInputStream) body, true);
            message.setContent(InputStream.class, body);
        }

        for (Attachment a : attachments.getLoadedAttachments()) {
            DataSource s = a.getDataHandler().getDataSource();
            if (!(s instanceof AttachmentDataSource)) {
                //AttachementDataSource objects are already cached
                cache((DelegatingInputStream) s.getInputStream(), false);
            }
        }
    }

    private void cache(DelegatingInputStream input, boolean deleteOnClose) throws IOException {
        if (loaded.contains(input)) {
            return;
        }
        loaded.add(input);
        CachedOutputStream out = null;
        InputStream origIn = input.getInputStream();
        try {
            out = new CachedOutputStream();
            setStreamedAttachmentProperties(out);
            IOUtils.copy(input, out);
            input.setInputStream(out.getInputStream());
            origIn.close();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Move the read pointer to the begining of the first part read till the end
     * of first boundary
     *
     * @param pushbackInStream
     * @param boundary
     * @throws MessagingException
     */
    private static boolean readTillFirstBoundary(PushbackInputStream pbs, byte[] bp) throws IOException {

        // work around a bug in PushBackInputStream where the buffer isn't
        // initialized
        // and available always returns 0.
        int value = pbs.read();
        pbs.unread(value);
        while (value != -1) {
            value = pbs.read();
            if ((byte) value == bp[0]) {
                int boundaryIndex = 0;
                while (value != -1 && (boundaryIndex < bp.length) && ((byte) value == bp[boundaryIndex])) {

                    value = pbs.read();
                    if (value == -1) {
                        throw new IOException("Unexpected End while searching for first Mime Boundary");
                    }
                    boundaryIndex++;
                }
                if (boundaryIndex == bp.length) {
                    // boundary found, read the newline
                    if (value == 13) {
                        pbs.read();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Create an Attachment from the MIME stream. If there is a previous attachment
     * that is not read, cache that attachment.
     *
     * @return
     * @throws IOException
     */
    private void setupAttachment(AttachmentImpl att, InternetHeaders headers) throws IOException {
        MimeBodyPartInputStream partStream = new MimeBodyPartInputStream(stream, boundary);

        final String ct = headers.getHeader("Content-Type", null);
        DataSource source = new AttachmentDataSource(ct, new DelegatingInputStream(partStream));
        att.setDataHandler(new DataHandler(source));

        for (Enumeration<?> e = headers.getAllHeaders(); e.hasMoreElements();) {
            Header header = (Header) e.nextElement();
            if (header.getName().equalsIgnoreCase("Content-Transfer-Encoding")
                            && header.getValue().equalsIgnoreCase("binary")) {
                att.setXOP(true);
            }
            att.setHeader(header.getName(), header.getValue());
        }
    }

    public boolean isLazyLoading() {
        return lazyLoading;
    }

    public void setLazyLoading(boolean lazyLoading) {
        this.lazyLoading = lazyLoading;
    }
}
