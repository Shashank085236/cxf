<?xml version="1.0"?>
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:xalan="http://xml.apache.org/xslt"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
    xmlns:wsse="http://schemas.xmlsoap.org/ws/2003/06/secext"
    xmlns:tns="http://objectweb.org/type_test"
    xmlns:itst="http://tests.iona.com/ittests">

  <xsl:output method="xml" indent="yes" xalan:indent-amount="4"/>
  <xsl:strip-space elements="*"/>

  <!-- Parameter: Path to the generated XSDs to include -->
  <xsl:param name="inc_xsd_path"/>
  <!-- Parameter: Use DOC-literal or RPC-literal style -->
  <xsl:param name="use_style"/>
 
  <!-- Variable: Reference schema document to include -->
  <!--xsl:variable name="reference_inc_doc" select="document('reference_inc.xsd')"/-->
 
  <!-- copy attributes from any node -->
  <xsl:template match="@*" mode="attribute_copy">
    <xsl:attribute name="{name(.)}">
      <xsl:value-of select="."/> 
    </xsl:attribute>
  </xsl:template>

  <!-- 0 - root schema node -->
  <xsl:template match="/xsd:schema">
    <definitions
        xmlns="http://schemas.xmlsoap.org/wsdl/"
        name="type_test">
  <!--      xmlns:ref="http://schemas.iona.com/references"  -->
      <xsl:apply-templates select="@*" mode="attribute_copy"/>
      <xsl:apply-templates select="." mode="schema"/>
      <xsl:apply-templates select="." mode="test_messages"/>
      <xsl:apply-templates select="." mode="test_portType"/>
    </definitions>
  </xsl:template>

  <!-- 1 - schema -->
  <xsl:template match="/xsd:schema" mode="schema"
        xmlns="http://schemas.xmlsoap.org/wsdl/">
    <types>
      <!--xsl:apply-templates select="$reference_inc_doc" mode="reference_inc"/-->
      <xsd:schema xmlns="http://www.w3.org/2001/XMLSchema">
        <xsl:apply-templates select="@*" mode="attribute_copy"/>
        <!--xsd:import namespace="http://schemas.iona.com/references"/-->
        <xsl:apply-templates select="itst:it_test_group" mode="schema_include"/>
        <xsl:apply-templates select="itst:it_test_group" mode="hardcoded_types"/>
        <xsl:apply-templates select="itst:it_test_group" mode="test_elements"/>
        <xsl:apply-templates select="itst:it_test_group" mode="schema_types"/>
      </xsd:schema>
    </types>
  </xsl:template>

  <!-- 1.1 - group of tests - schema reference inclusion -->
  <!--xsl:template match="/xsd:schema" mode="reference_inc">
    <xsd:schema xmlns="http://www.w3.org/2001/XMLSchema">
      <xsl:apply-templates select="@*" mode="attribute_copy"/>
      <xsl:copy-of select="*"/>
    </xsd:schema>
  </xsl:template-->
  
  <!-- 1.2 - group of tests - schema include -->
  <xsl:template match="itst:it_test_group[@ID]" mode="schema_include">
    <xsd:include>
      <xsl:attribute name="schemaLocation">
        <xsl:value-of select="concat($inc_xsd_path, '/type_test_', @ID, '.xsd')"/> 
      </xsl:attribute>
    </xsd:include>
  </xsl:template>

  <!-- 1.2.5 - group of tests - schema include -->
  <xsl:template match="itst:it_test_group[not(@ID)]" mode="hardcoded_types">
    <!--
    <xsd:element name="testVoid"/>
    <xsd:element name="testOneway">
      <xsd:complexType>
        <sequence>
          <element name="x" type="xsd:string"/>
          <element name="y" type="xsd:string"/>
        </sequence>
      </xsd:complexType>
    </xsd:element>
    -->
  </xsl:template>
  
  <!-- 1.3 group of types (only for groups with no ID) -->
  <xsl:template match="itst:it_test_group[not(@ID)]" mode="schema_types">
    <xsl:apply-templates select="xsd:attribute" mode="schema_type"/>
    <xsl:apply-templates select="xsd:attributeGroup" mode="schema_type"/>
    <xsl:apply-templates select="xsd:group" mode="schema_type"/>
    <xsl:apply-templates select="xsd:simpleType" mode="schema_type"/>
    <xsl:apply-templates select="xsd:complexType" mode="schema_type"/>
    <xsl:apply-templates select="xsd:element" mode="schema_type"/>
  </xsl:template>
  
  <!-- 1.3.1 - schema type or construct -->
  <xsl:template match="itst:it_test_group/*" mode="schema_type" xmlns="http://www.w3.org/2001/XMLSchema">
      <xsl:element name="{name(.)}">
        <!-- drop "it_no_test" from the attributes -->
        <xsl:apply-templates select="@*[name()!='itst:it_no_test']" mode="attribute_copy"/>
        <xsl:copy-of select="*"/>
      </xsl:element>
  </xsl:template>

  <!-- 1.4 - group of tests - test elements -->
  <xsl:template match="itst:it_test_group" mode="test_elements">
    <xsl:if test="$use_style='document'">
      <!--xsl:apply-templates select="xsd:simpleType" mode="doc_elements_xyz"/>
      <xsl:apply-templates select="xsd:complexType" mode="doc_elements_xyz"/>
      <xsl:apply-templates select="xsd:element" mode="doc_elements_xyz"/-->
      <xsl:apply-templates select="itst:builtIn" mode="doc_elements_xyz"/>
    </xsl:if>
    <xsl:if test="$use_style='rpc'">
      <xsl:apply-templates select="xsd:simpleType" mode="elements_xyz"/>
      <xsl:apply-templates select="xsd:complexType" mode="elements_xyz"/>
      <xsl:apply-templates select="xsd:element" mode="elements_xyz"/>
      <xsl:apply-templates select="itst:builtIn" mode="elements_xyz"/>
    </xsl:if>
  </xsl:template>

  <!-- 1.4.1 - group of x/y/z/return test elements -->
  <xsl:template match="itst:it_test_group/*[not(@itst:it_no_test='true')]" mode="elements_xyz">
    <xsl:apply-templates select="." mode="test_element">
      <xsl:with-param name="suffix">_x</xsl:with-param>
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="test_element">
      <xsl:with-param name="suffix">_y</xsl:with-param>
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="test_element">
      <xsl:with-param name="suffix">_z</xsl:with-param>
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="test_element">
      <xsl:with-param name="suffix">_return</xsl:with-param>
    </xsl:apply-templates>
  </xsl:template>

  <!-- 1.4.1.1 - test element for simpleType or complexType -->
  <xsl:template match="itst:it_test_group/xsd:simpleType|xsd:complexType" mode="test_element">
    <xsl:param name="suffix"/>
    <xsd:element>
      <xsl:attribute name="type">
        <xsl:value-of select="concat('tns:',@name)"/>
      </xsl:attribute>
      <xsl:apply-templates select="." mode="test_element_name">
        <xsl:with-param name="suffix" select="$suffix"/>
      </xsl:apply-templates>
    </xsd:element>
  </xsl:template>

  <!-- 1.4.1.2 - test element for global element -->
  <xsl:template match="itst:it_test_group/xsd:element" mode="test_element">
    <xsl:param name="suffix"/>
    <xsd:element>
      <xsl:apply-templates select="." mode="test_element_name">
        <xsl:with-param name="suffix" select="$suffix"/>
      </xsl:apply-templates>
      <xsl:apply-templates select="@*[name()!='name']" mode="attribute_copy"/>
      <xsl:copy-of select="*"/>
    </xsd:element>
  </xsl:template>

  <!-- 1.4.1.3 - test element for built-in type -->
  <xsl:template match="itst:it_test_group/itst:builtIn" mode="test_element">
    <xsl:param name="suffix"/>
    <xsd:element>
      <xsl:attribute name="type">
        <xsl:value-of select="concat(@prefix, @name)"/>
      </xsl:attribute>
      <xsl:apply-templates select="." mode="test_element_name">
        <xsl:with-param name="suffix" select="$suffix"/>
      </xsl:apply-templates>
    </xsd:element>
  </xsl:template>

  <!-- 1.4.1.x.1 - test element name -->
  <xsl:template match="itst:it_test_group/*" mode="test_element_name">
    <xsl:param name="suffix"/>
    <xsl:attribute name="name">
      <xsl:value-of select="concat(@name,$suffix)"/>
    </xsl:attribute>
  </xsl:template>

  <!-- 1.4.1.a - group of x/y/z/return test elements -->
  <xsl:template match="itst:it_test_group/*[not(@itst:it_no_test='true')]" mode="doc_elements_xyz">
    <xsl:variable name="prefix">
      <xsl:value-of select="'xsd:'"/>
    </xsl:variable>
    <xsl:variable name="operation_name">
      <xsl:value-of select="concat('test',
                            concat(translate(substring(@name, 1, 1),    
                                   'abcdefghijklmnopqrstuvwxyz', 
                                   'ABCDEFGHIJKLMNOPQRSTUVWXYZ'),
                                   substring(@name, 2)))"/>
    </xsl:variable>
    <xsl:variable name="operation_output_name">
      <xsl:value-of select="concat($operation_name, 'Response')"/>
    </xsl:variable>
    <xsd:element>
      <xsl:attribute name="name">
        <xsl:value-of select="$operation_name"/>
      </xsl:attribute>
      <xsd:complexType>
        <sequence>
          <xsd:element>
            <xsl:attribute name="name">
              <xsl:value-of select="'x'"/>
            </xsl:attribute>
            <xsl:attribute name="type">
              <xsl:value-of select="concat($prefix, @name)"/>
            </xsl:attribute>
          </xsd:element>
          <xsd:element>
            <xsl:attribute name="name">
              <xsl:value-of select="'y'"/>
            </xsl:attribute>
            <xsl:attribute name="type">
              <xsl:value-of select="concat($prefix, @name)"/>
            </xsl:attribute>
          </xsd:element>
        </sequence>
      </xsd:complexType>
    </xsd:element>
    <xsd:element>
      <xsl:attribute name="name">
        <xsl:value-of select="$operation_output_name"/>
      </xsl:attribute>
      <xsd:complexType>
        <sequence>
          <xsd:element>
            <xsl:attribute name="name">
              <xsl:value-of select="'return'"/>
            </xsl:attribute>
            <xsl:attribute name="type">
              <xsl:value-of select="concat($prefix, @name)"/>
            </xsl:attribute>
          </xsd:element>
          <xsd:element>
            <xsl:attribute name="name">
              <xsl:value-of select="'y'"/>
            </xsl:attribute>
            <xsl:attribute name="type">
              <xsl:value-of select="concat($prefix, @name)"/>
            </xsl:attribute>
          </xsd:element>
          <xsd:element>
            <xsl:attribute name="name">
              <xsl:value-of select="'z'"/>
            </xsl:attribute>
            <xsl:attribute name="type">
              <xsl:value-of select="concat($prefix, @name)"/>
            </xsl:attribute>
          </xsd:element>
        </sequence>
      </xsd:complexType>
    </xsd:element>
  </xsl:template>

  <!-- 2 - test messages -->
  <xsl:template match="/xsd:schema" mode="test_messages">
    <xsl:apply-templates select="." mode="hardcoded_messages"/>
    <xsl:apply-templates select="itst:it_test_group" mode="test_messages_group"/>
  </xsl:template>

  <!-- 2.1 - hardcoded messages -->
  <xsl:template match="/xsd:schema" mode="hardcoded_messages"
        xmlns="http://schemas.xmlsoap.org/wsdl/">
    <!--
    <message name="testVoid"/>
    <message name="testDispatch">
      <part name="method_name" element="tns:string_return"/>
    </message>
    <message name="testOneway">
        <xsl:if test="$use_style='document'">
          <xsl:attribute name="name">
            <xsl:value-of select="'testOneway'"/>
          </xsl:attribute>
          <part name="in" element="tns:testOneway"/>
        </xsl:if>
        <xsl:if test="$use_style='rpc'">
          <xsl:attribute name="name">
            <xsl:value-of select="'testOnewayRequest'"/>
          </xsl:attribute>
          <part name="x" element="tns:string_x"/>
          <part name="y" element="tns:string_y"/>
        </xsl:if>
    </message>
    -->
  </xsl:template>

  <!-- 2.2 - group of test messages -->
  <xsl:template match="itst:it_test_group" mode="test_messages_group">
    <xsl:apply-templates select="xsd:simpleType" mode="test_messages"/>
    <xsl:apply-templates select="xsd:complexType" mode="test_messages"/>
    <xsl:apply-templates select="xsd:element" mode="test_messages"/>
    <xsl:apply-templates select="itst:builtIn" mode="test_messages"/>
  </xsl:template>
  
  <!-- 2.2.1 - request and response messages -->
  <xsl:template match="itst:it_test_group/*[not(@itst:it_no_test='true')]" mode="test_messages"
        xmlns="http://schemas.xmlsoap.org/wsdl/">
    <xsl:variable name="message_name_prefix">
      <xsl:value-of select="concat('test',
                                   concat(translate(substring(@name, 1, 1),
                                          'abcdefghijklmnopqrstuvwxyz', 
                                          'ABCDEFGHIJKLMNOPQRSTUVWXYZ'),
                                          substring(@name, 2)))"/>
    </xsl:variable>
    <xsl:variable name="input_message_name">
      <xsl:value-of select="$message_name_prefix"/>
    </xsl:variable>
    <xsl:variable name="output_message_name">
      <xsl:value-of select="concat($message_name_prefix,'Response')"/>
    </xsl:variable>
    <xsl:variable name="part_name_prefix">
      <xsl:value-of select="concat('tns:',@name)"/>
    </xsl:variable>
    <message>
      <xsl:attribute name="name">
        <xsl:value-of select="$input_message_name"/>
      </xsl:attribute>
  <xsl:if test="$use_style='document'">
      <part>
        <xsl:attribute name="name">in</xsl:attribute>
        <xsl:attribute name="element">
          <xsl:value-of select="concat('tns:', $input_message_name)"/>
        </xsl:attribute>
      </part>
  </xsl:if>
  <xsl:if test="$use_style='rpc'">
      <part>
        <xsl:attribute name="name">x</xsl:attribute>
        <xsl:attribute name="element">
          <xsl:value-of select="concat($part_name_prefix, '_x')"/>
        </xsl:attribute>
      </part>
      <part>
        <xsl:attribute name="name">y</xsl:attribute>
        <xsl:attribute name="element">
          <xsl:value-of select="concat($part_name_prefix, '_y')"/>
        </xsl:attribute>
      </part>
  </xsl:if>
    </message>
    <message>
      <xsl:attribute name="name">
        <xsl:value-of select="$output_message_name"/>
      </xsl:attribute>
  <xsl:if test="$use_style='document'">
      <part>
        <xsl:attribute name="name">out</xsl:attribute>
        <xsl:attribute name="element">
          <xsl:value-of select="concat('tns:', $output_message_name)"/>
        </xsl:attribute>
      </part>
  </xsl:if>
  <xsl:if test="$use_style='rpc'">
      <part>
        <xsl:attribute name="name">return</xsl:attribute>
        <xsl:attribute name="element">
          <xsl:value-of select="concat($part_name_prefix, '_return')"/>
        </xsl:attribute>
      </part>
      <part>
        <xsl:attribute name="name">y</xsl:attribute>
        <xsl:attribute name="element">
          <xsl:value-of select="concat($part_name_prefix, '_y')"/>
        </xsl:attribute>
      </part>
      <part>
        <xsl:attribute name="name">z</xsl:attribute>
        <xsl:attribute name="element">
          <xsl:value-of select="concat($part_name_prefix, '_z')"/>
        </xsl:attribute>
      </part>
  </xsl:if>
    </message>
  </xsl:template>

  <!-- 3 - test portType -->
  <xsl:template match="/xsd:schema" mode="test_portType"
        xmlns="http://schemas.xmlsoap.org/wsdl/">
    <portType name="TypeTestPortType">
      <xsl:apply-templates select="." mode="hardcoded_operations"/>
      <xsl:apply-templates select="itst:it_test_group" mode="test_operations_group"/>
    </portType>
  </xsl:template>

  <!-- 3.1 - hardcoded operations -->
  <xsl:template match="/xsd:schema" mode="hardcoded_operations"
        xmlns="http://schemas.xmlsoap.org/wsdl/">
    <!--
    <operation name="testVoid">
      <input message="tns:testVoid" name="testVoid"/>
    </operation>
    <operation name="testOneway">
      <input message="tns:testOneway" name="testOneway">
        <xsl:if test="$use_style='document'">
          <xsl:attribute name="message">
            <xsl:value-of select="'tns:testOneway'"/>
          </xsl:attribute>
          <xsl:attribute name="name">
            <xsl:value-of select="'testOneway'"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:if test="$use_style='rpc'">
          <xsl:attribute name="message">
            <xsl:value-of select="'tns:testOnewayRequest'"/>
          </xsl:attribute>
          <xsl:attribute name="name">
            <xsl:value-of select="'testOnewayRequest'"/>
          </xsl:attribute>
        </xsl:if>
      </input>
    </operation>
    -->
    <!--
    <operation name="testDispatch1">
      <input message="tns:testDispatch" name="testDispatch1"/>
      <output message="tns:testDispatch" name="testDispatch1Response"/>
    </operation>
    <operation name="testDispatch2">
      <input message="tns:testDispatch" name="testDispatch2"/>
      <output message="tns:testDispatch" name="testDispatch2Response"/>
    </operation>
    -->
  </xsl:template>

  <!-- 3.2 - group of test operations -->
  <xsl:template match="itst:it_test_group" mode="test_operations_group">
    <!--xsl:apply-templates select="xsd:simpleType" mode="test_operation"/>
    <xsl:apply-templates select="xsd:complexType" mode="test_operation"/>
    <xsl:apply-templates select="xsd:element" mode="test_operation"/-->
    <xsl:apply-templates select="itst:builtIn" mode="test_operation"/>
  </xsl:template>
  
  <!-- 3.2.1 - test operations -->
  <xsl:template match="itst:it_test_group/*[not(@itst:it_no_test='true')]" mode="test_operation"
        xmlns="http://schemas.xmlsoap.org/wsdl/">
    <xsl:variable name="operation_name">
      <xsl:value-of select="concat('test',
                                   concat(translate(substring(@name, 1, 1),
                                          'abcdefghijklmnopqrstuvwxyz', 
                                          'ABCDEFGHIJKLMNOPQRSTUVWXYZ'),
                                          substring(@name, 2)))"/>
    </xsl:variable>
    <xsl:variable name="operation_input_name">
      <xsl:value-of select="$operation_name"/>
    </xsl:variable>
    <xsl:variable name="operation_output_name">
      <xsl:value-of select="concat($operation_name, 'Response')"/>
    </xsl:variable>
    <xsl:variable name="message_name_prefix">
      <xsl:value-of select="concat('tns:', $operation_name)"/>
    </xsl:variable>
    <xsl:variable name="input_message_name">
      <xsl:value-of select="$message_name_prefix"/>
    </xsl:variable>
    <xsl:variable name="output_message_name">
      <xsl:value-of select="concat($message_name_prefix,'Response')"/>
    </xsl:variable>
    <operation>
      <xsl:attribute name="name">
        <xsl:value-of select="$operation_name"/>
      </xsl:attribute>
      <input>
        <xsl:attribute name="name">
          <xsl:value-of select="$operation_input_name"/>
        </xsl:attribute>
        <xsl:attribute name="message">
          <xsl:value-of select="$input_message_name"/>
        </xsl:attribute>
      </input>
      <output>
        <xsl:attribute name="name">
          <xsl:value-of select="$operation_output_name"/>
        </xsl:attribute>
        <xsl:attribute name="message">
          <xsl:value-of select="$output_message_name"/>
        </xsl:attribute>
      </output>
    </operation>
  </xsl:template>

</xsl:stylesheet>

