/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.config.javamail;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * The Class UserAuth.
 * 
 * @author <a href="mailto:agalue@opennms.org">Alejandro Galue</a>
 */
@XmlRootElement(name="user-auth", namespace="http://xmlns.opennms.org/xsd/config/javamail-configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserAuth implements java.io.Serializable {

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -107408608071332141L;

    /** The user name. */
    @XmlAttribute(name="user-name")
    private String _userName;

    /** The password. */
    @XmlAttribute(name="password")
    private String _password;

    //----------------/
    //- Constructors -/
    //----------------/

    /**
     * Instantiates a new user auth.
     */
    public UserAuth() {
        super();
    }

    //-----------/
    //- Methods -/
    //-----------/

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override()
    public boolean equals(final Object obj) {
        if ( this == obj )
            return true;
        if (obj instanceof UserAuth) {
            UserAuth temp = (UserAuth)obj;
            if (this._userName != null) {
                if (temp._userName == null) return false;
                else if (!(this._userName.equals(temp._userName))) 
                    return false;
            }
            else if (temp._userName != null)
                return false;
            if (this._password != null) {
                if (temp._password == null) return false;
                else if (!(this._password.equals(temp._password))) 
                    return false;
            }
            else if (temp._password != null)
                return false;
            return true;
        }
        return false;
    }

    /**
     * Returns the value of field 'password'.
     * 
     * @return the value of field 'Password'.
     */
    public String getPassword() {
        return this._password == null ? "opennms" : this._password;
    }

    /**
     * Returns the value of field 'userName'.
     * 
     * @return the value of field 'UserName'.
     */
    public String getUserName() {
        return this._userName == null ? "opennms" : this._userName;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int result = 17;
        if (_userName != null) {
            result = 37 * result + _userName.hashCode();
        }
        if (_password != null) {
            result = 37 * result + _password.hashCode();
        }
        return result;
    }

    /**
     * Sets the value of field 'password'.
     * 
     * @param password the value of field 'password'.
     */
    public void setPassword(final String password) {
        this._password = password;
    }

    /**
     * Sets the value of field 'userName'.
     * 
     * @param userName the value of field 'userName'.
     */
    public void setUserName(final String userName) {
        this._userName = userName;
    }

}
