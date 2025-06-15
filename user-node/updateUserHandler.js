import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import { DynamoDBDocumentClient, UpdateCommand } from "@aws-sdk/lib-dynamodb";

const client = new DynamoDBClient({ region: process.env.AWS_REGION });
const docClient = DynamoDBDocumentClient.from(client);

export const updateUserHandler = async (event) => {
  const { id } = event.pathParameters || {};

  if (!id) {
    return {
      statusCode: 400,
      body: JSON.stringify({ error: "Missing path parameter 'id'" }),
    };
  }

  let updates;
  try {
    updates = JSON.parse(event.body);
  } catch {
    return {
      statusCode: 400,
      body: JSON.stringify({ error: "Invalid JSON in request body" }),
    };
  }

  const exprNames = {};
  const exprValues = {};
  const setClauses = [];

  if (updates.name) {
    exprNames["#name"] = "name";
    exprValues[":name"] = updates.name;
    setClauses.push("#name = :name");
  }
  if (updates.email) {
    exprNames["#email"] = "email";
    exprValues[":email"] = updates.email;
    setClauses.push("#email = :email");
  }

  if (setClauses.length === 0) {
    return {
      statusCode: 400,
      body: JSON.stringify({ error: "No valid fields to update" }),
    };
  }

  const params = {
    TableName: process.env.DYNAMO_TABLE,
    Key: { id },
    UpdateExpression: "SET " + setClauses.join(", "),
    ExpressionAttributeNames: exprNames,
    ExpressionAttributeValues: exprValues,
    ReturnValues: "ALL_NEW",
  };

  try {
    const result = await docClient.send(new UpdateCommand(params));
    return {
      statusCode: 200,
      body: JSON.stringify({
        message: "User updated",
        user: result.Attributes,
      }),
    };
  } catch (err) {
    console.error("Error updating user:", err);
    return {
      statusCode: 500,
      body: JSON.stringify({ error: "Internal server error" }),
    };
  }
};
