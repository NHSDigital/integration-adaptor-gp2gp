// this file gets executed for every message to be sent
// the `entry` variable is provided by the "A" tool and it's value comes from the lines of the file configured using the `-W` parameter
// this code replaces the `{NHS_NUMBER}` placeholder from the message template (example.xml) with values form `-W` file for each message
msg.body=msg.body.replace('{NHS_NUMBER}', entry); 

// UUID generator taken from https://stackoverflow.com/a/44078785/249136 
var u = Date.now().toString(16) + Math.random().toString(16) + '0'.repeat(16); 
var guid = [u.substr(0,8), u.substr(8,4), '4000-8' + u.substr(13,3), u.substr(16,12)].join('-'); 
msg.stringProperties.put('ConversationId', guid);