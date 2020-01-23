/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2014.02.04 at 01:34:24 PM CET
//


package com.evolveum.prism.xml.ns._public.types_3;

import com.evolveum.midpoint.prism.JaxbVisitable;
import com.evolveum.midpoint.prism.JaxbVisitor;

import java.io.Serializable;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 *
 *                 TODO
 *
 *                 Loosely based on XML encryption standard. But we cannot use full
 *                 standard as we are not bound to XML. We need this to work also for
 *                 JSON and YAML and other languages.
 *
 *
 * <p>Java class for CipherDataType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CipherDataType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="cipherValue" type="{http://www.w3.org/2001/XMLSchema}base64Binary"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CipherDataType", propOrder = {
    "cipherValue"
})
public class CipherDataType implements Serializable, Cloneable, JaxbVisitable {

    @XmlElement(required = true)
    protected byte[] cipherValue;

    /**
     * Gets the value of the cipherValue property.
     *
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getCipherValue() {
        return cipherValue;
    }

    /**
     * Sets the value of the cipherValue property.
     *
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setCipherValue(byte[] value) {
        this.cipherValue = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(cipherValue);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CipherDataType other = (CipherDataType) obj;
        if (!Arrays.equals(cipherValue, other.cipherValue))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CipherDataType(cipherValue=" + (cipherValue==null?"null":"["+cipherValue.length+" bytes]") + ")";
    }

    @Override
    public CipherDataType clone() {
        CipherDataType cloned = new CipherDataType();
        cloned.setCipherValue(cipherValue.clone());
        return cloned;
    }

    @Override
    public void accept(JaxbVisitor visitor) {
        visitor.visit(this);
    }
}
