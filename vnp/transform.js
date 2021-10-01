msg.body=msg.body.replace('{NHS_NUMBER}', entry); 

var u = Date.now().toString(16) + Math.random().toString(16) + '0'.repeat(16); 
var guid = [u.substr(0,8), u.substr(8,4), '4000-8' + u.substr(13,3), u.substr(16,12)].join('-'); 
msg.stringProperties.put('ConversationId', guid);