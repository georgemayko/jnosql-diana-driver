/*
 *  Copyright (c) 2018 Otávio Santana and others
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.diana.dynamodb;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class DynamoTableUtils {

    private static final long READ_CAPACITY_UNITS = 5L;

    private DynamoTableUtils() {
    }

    public static KeySchemaElement createKeyElementSchema(Map<String, KeyType> keys) {

        KeySchemaElement.Builder keySchemaElementBuilder = KeySchemaElement.builder();

        keys
                .entrySet()
                .forEach(
                        es -> {
                            keySchemaElementBuilder.attributeName(es.getKey());
                            keySchemaElementBuilder.keyType(es.getValue());
                        });

        return keySchemaElementBuilder.build();
    }

    public static AttributeDefinition createAttributeDefinition(Map<String, ScalarAttributeType> attributes) {

        AttributeDefinition.Builder attributeDefinitionBuilder = AttributeDefinition.builder();

        attributes
                .entrySet()
                .forEach(
                        es -> {
                            attributeDefinitionBuilder.attributeName(es.getKey());
                            attributeDefinitionBuilder.attributeType(es.getValue());
                        }
                );

        return attributeDefinitionBuilder.build();
    }

    public static ProvisionedThroughput createProvisionedThroughput(Long readCapacityUnits, Long writeCapacityUnit) {

        ProvisionedThroughput.Builder provisionedThroughputBuilder = ProvisionedThroughput.builder();

        if (readCapacityUnits != null && readCapacityUnits.longValue() > 0)
            provisionedThroughputBuilder.readCapacityUnits(readCapacityUnits);
        else {
            provisionedThroughputBuilder.readCapacityUnits(READ_CAPACITY_UNITS);
        }


        if (writeCapacityUnit != null && writeCapacityUnit.longValue() > 0)
            provisionedThroughputBuilder.writeCapacityUnits(writeCapacityUnit);
        else {
            provisionedThroughputBuilder.writeCapacityUnits(READ_CAPACITY_UNITS);
        }


        return provisionedThroughputBuilder.build();
    }

    public static Map<String, KeyType> createKeyDefinition() {
        return Collections.singletonMap(ConfigurationAmazonEntity.KEY, KeyType.HASH);
    }

    public static Map<String, ScalarAttributeType> createAttributesType() {
        return Collections.singletonMap(ConfigurationAmazonEntity.KEY, ScalarAttributeType.S);
    }

    public static void manageTables(String tableName, DynamoDbClient client, Long readCapacityUnits, Long writeCapacityUnit) {

        boolean hasTable = true;
        String lastName = null;

        while (hasTable) {
            try {
                ListTablesResponse response = null;
                if (lastName == null) {
                    ListTablesRequest request = ListTablesRequest.builder().build();
                    response = client.listTables(request);
                } else {
                    ListTablesRequest request = ListTablesRequest.builder().exclusiveStartTableName(lastName).build();
                    response = client.listTables(request);
                }

                List<String> tableNames = response.tableNames();

                if (tableNames.size() == 0) {
                    createTable(tableName, client, readCapacityUnits, writeCapacityUnit);
                } else {
                    lastName = response.lastEvaluatedTableName();
                    if (lastName == null) {
                        hasTable = false;
                    }
                }
            } catch (DynamoDbException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void createTable(String tableName, DynamoDbClient client, Long readCapacityUnits, Long writeCapacityUnit) {

        Map<String, KeyType> keyDefinition = createKeyDefinition();
        Map<String, ScalarAttributeType> attributeDefinition = createAttributesType();

        client.createTable(CreateTableRequest.builder()
                .tableName(tableName)
                .provisionedThroughput(createProvisionedThroughput(readCapacityUnits, writeCapacityUnit))
                .keySchema(createKeyElementSchema(keyDefinition))
                .attributeDefinitions(createAttributeDefinition(attributeDefinition))
                .build());
    }
}
