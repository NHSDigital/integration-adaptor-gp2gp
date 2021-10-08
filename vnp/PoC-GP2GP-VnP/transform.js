var u = Date.now().toString(16) + Math.random().toString(16) + '0'.repeat(16); 
var guid = [u.substr(0,8), u.substr(8,4), '4000-8' + u.substr(13,3), u.substr(16,12)].join('-'); 

msg.body=msg.body.replace('{CONVERSATION_ID}', guid); 
msg.body=msg.body.replace('{NHS_NUMBER}', entry); // entry is taken from nhs_numbers.txt