## Vtfr
#### Votifier with HMACs instead of RSA

The protocol is fairly simple

Build your data into the following structure:
```
VOTE\n
<serverListName>\n
<username>\n
<userIPAddress>\n
<timestampInMilliseconds>
```

Create a SHA1 HMAC of that string using the key given to you by the server.

Append `\n<HMAC>\n` to the string so you end up with something that looks like this:

```
VOTE\n
<serverListName>\n
<username>\n
<userIPAddress>\n
<timestampInMilliseconds>\n
<SHA1-HMAC>\n
```

Connect over TCP to the Vtfr server host and port. Send the string. Close connection.
The server will not send any acknowledgement of the request.

This request can be done over HTTP as well. Simply send the data as the body of a `POST` request.
The HTTP headers will be ignored and the body will be used as the VOTE packet.
Make sure you set a `Content-Lenght` header so the request doesn't use chunked encoding. Vtfr doesn't support that.