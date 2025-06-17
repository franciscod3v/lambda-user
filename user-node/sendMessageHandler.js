const { SNSClient, PublishCommand } = require("@aws-sdk/client-sns");

const snsTopicArn = process.env.SNS_TOPIC_ARN;
const snsClient = new SNSClient({ region: process.env.AWS_REGION });

async function sendMessageHandler(event) {
  const publishPromises = [];

  for (const record of event.Records) {
    try {
      const { name, email } = JSON.parse(record.body);
      const message = `User with name ${name} and email ${email} was created.`;
      const publishCmd = new PublishCommand({ TopicArn: snsTopicArn, Message: message });
      publishPromises.push(snsClient.send(publishCmd));
    } catch (error) {
      console.error("Error processing record:", error);
    }
  }

  await Promise.all(publishPromises);

  return {
    statusCode: 200,
    body: JSON.stringify({ message: "Sent via SNS." }),
  };
}

module.exports = { sendMessageHandler };