export default ({
  recipients,
  body,
  subject,
  attachments,
}) => ({
  address: recipients.join(';'),
  body,
  subject,
  attachment: attachments[0],
});
