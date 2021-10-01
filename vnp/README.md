# Volumetric and Performance testing

GP2GP performance tests can be performed by putting messages directly onto MHS inbound queue from where GP2GP consumes them

## Generating NHS numbers source file

Sending multiple messages with different NHS number requires a source file with NHS number in each line.
Following script allows generating such source file.

    python nhs_number_source_file_generator.py <output_file_name> "<input_data_dictionary>"

Parameters:
- `<output_file_name>` - target file where nhs numbers will be saved to
- `<input_data_dictionary>` - python-compliant input dictionary with nhs numbers as keys and positive integers as values indicating the number of given nhs numbers to generate

Example:

    python nhs_number_source_file_generator.py nhs_numbers_generated.txt "{'123':1,'234':2,'345':3}";

This will create `nhs_numbers_generated.txt` file with `1 x 123`, `2 x 234` and `3 x 345` numbers in random order. Example output:

    234
    345
    123
    345
    345
    234

## Sending messages to `MHS Inbound Queue`

Use the ["A" application](https://github.com/fmtn/a) which is an AMQP client allowing you to send messages directly to `MHS Inbound Queue`. Java 11 required.

## PoC:

Assuming ActiveMQ has been started and listening on port `5672`.
Localhost container can be started by running following command from GP2GP `./docker/` folder:

    docker-compose build activemq; docker-compose up activemq


### Sending messages

Parameters:
- `-A` - set protocol to AMQP
- `-p` - payload to send. `@` to use a file as source
- `-b` - URL to ActiveMQ broker
- `-S` - javascript to fill in the template with data from file and to generate random ConversationId on each message. `@` to use a file as source
- `-W` - path to file with values to use when replacing placeholder in template. The number of lines in this file also defines how many messages will be sent. `-W` can't be used with `-c` (number of messages to send) parameter

Example:

    java -jar a-1.5.0-jar-with-dependencies.jar \
        -A \
        -b "amqp://admin:admin@localhost:5672" \
        -p "@example.xml" \
        -S "@transform.js" \
        -W "example_nhs_numbers.txt" \
        inbound_queue


This will send the content of the `example.xml` file to the `inbound_queue` on `localhost` broker listening on port `5672` connecting using unsecured AMQP protocol.

There will be 3 messages each having different NHS number, because the source `example_nhs_numbers.txt` file has 3 lines each having different value.

When connecting to AMQP broker using a secured connection, `amqps://` protocol should be used instead of `amqp://`

Using https://stackoverflow.com/a/44078785/249136 for generating UUID in javascript

### Downloading messages

For PoC testing purpose only to download messages and confirm that all messages have different NHS number and each has a randomly generated ConversationId string property.

Parameters:
- `-A` - set protocol to AMQP
- `-b` - URL to ActiveMQ broker
- `-c` - how many messages to get
- `-g` - get messages
- `-j` - print JMS headers

Following example will download first 3 messages from the queue

    java -jar a-1.5.0-jar-with-dependencies.jar \
        -A \
        -b "amqp://admin:admin@localhost:5672" \
        -c 3 \
        -g \
        -j \
        inbound_queue