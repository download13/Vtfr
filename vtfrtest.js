var net = require('net');
var http = require('http');
var crypto = require('crypto');

var list = 'mineshafter_list_0';
var key = 'testkeyformineshafter';
var body = ['VOTE', list, 'download', '1.1.0.0', Date.now()].join('\n');
var hm = crypto.createHmac('sha1', key);
hm.update(body);
var hmac = hm.digest('hex');
body += '\n' + hmac + '\n';

var conn = net.connect({port: 25560}, function() {
	conn.write(body);
	conn.end();
});

var req = http.request({
	port: 25560,
	method: 'POST',
	headers: {
		'Content-Length': body.length
	}
});
req.end(body);
req.on('error', console.log);