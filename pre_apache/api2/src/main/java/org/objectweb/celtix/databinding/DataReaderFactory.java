package org.objectweb.celtix.databinding;


//REVISIT - need to move the Reader/Writer stuff, also probably 
//need the MessageInfo/OperationInfo as a param 
public interface DataReaderFactory {
    Class<?>[] getSupportedFormats();
    
    <T> DataReader<T> createReader(Class<T> cls);
}
