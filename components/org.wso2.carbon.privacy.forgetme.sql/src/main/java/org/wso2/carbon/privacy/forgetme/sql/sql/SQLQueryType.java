/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.carbon.privacy.forgetme.sql.sql;

/**
 * Represents an SQL query type.
 */
public enum SQLQueryType {

    // SQL queries that has the domain name appended to the username.
    DOMAIN_APPENDED,

    // SQL queries that has the domain name separate (In a different column) from the username.
    DOMAIN_SEPARATED,

    // SQL queries that has tenant domain appended to the username except for super tenant.
    TENANT_SPECIFIC_APPENDED,

    // SQL queries that has tenant domain appended to the username.
    TENANT_APPENDED

}
