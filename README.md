# Smtp Client

This module allows to send raw emails via Smtp.

This is a a real async module (no worker needed).

## Name

The module name is `smtpclient`.

## Configuration

## Sending a message

Command can be used to run more advanced MongoDB features, such as using Mapreduce.
There is a complete list of commands at http://docs.mongodb.org/manual/reference/command/.

The smtpclient module takes the following configuration:

    {
        "host": <host>,
        "port": <port>,
        "helo": <helo>,
        "pipelining": <pipelining>,
        "connection_timeout": <connection_timeout>
        "sender": <sender>
        "recipients": <recipients>
        "message": <message>
    }

For example:

    {
        "host": "192.168.1.100",
        "port": 25,
        "helo", "localhost"
        "pipelining": "TRY",
        "connection_timout": 5
        "sender": "foo@bar.de",
        "recpients", ["rcpt1@bar2.de", "rcpt2@bar2.de"],
        "message": "U3ViamVjdDogdGVzdA0KDQpUZXN0DQo="
    }

Let's take a look at each field in turn:

* `host` Host name or ip address of the SMTP server to use.
* `port` Port at which the SMTP server is listening. Defaults to `25`.
* `helo` Name which is transmit during the HELO/EHLO command
* `pipelining` Define if SMTP PIPELINING should be used. Possible values are: TRY, DEPEND, NO. Default value is TRY.
* `connection_timeout` Set the connection timeout in seconds.
* `sender' The sender which will be used for the MAIL FROM command. Default null
* `recipients` The recipients which are used for the RCPT TO commands.
* `message` The message to send (as byte array).



An example that just pings to make sure the mongo instance is up would be:

    {
        "host":"localhost",
        "port":1025,"helo":
        "localhost","pipeling":"TRY",
        "connection_timeout":5,
        "sender":"foo@bar.de",
        "recipients":["foo@bar.de","foo2@bar.de"],
        "message":"U3ViamVjdDogdGVzdA0KDQpUZXN0DQo="}
    }

You would expect a result something like for a connection refused, which means an error during delivery:

    {
        "status":"error",
        "message":"Error while deliver messages: java.net.ConnectException: Connection refused: localhost/127.0.0.1:1025"
    }

If the delivery itself had no error but recipients was rejected you would see something like:

    {
        "deliveryStatus":[{
                            "recipient":"invalid@invalid.de",
                            "status":"PermanentError",
                            "response":"553 sorry, that domain isn't in my list of allowed rcpthosts (#5.7.1)"
                          }
                          ,
                          {
                            "recipient":"invali2d@invalid.de",
                            "status":"PermanentError",
                            "response":"553 sorry, that domain isn't in my list of allowed rcpthosts (#5.7.1)"
                          }],
        "status":"ok"}

If the delivery itself had no error and the emails was accepted for the recipients you would see:

    {
        "deliveryStatus":[{
                            "recipient":"valid@valid.de",
                            "status":"Ok",
                            "response":"250 ok 1375953438 qp 6971"
                          }
                          ,
                          {
                            "recipient":"valid2@valid.de",
                            "status":"Ok",
                            "response":"250 ok 1375953438 qp 6971"
                          }],
        "status":"ok"}
