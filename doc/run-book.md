## Setup Datomic on AWS

Requirements:
- AWS account (free tier is fine but after this it will not be free anymore)
- AWS credentials for AWS CLI access
- choose the right region for your needs (default will be us-east-1)
- datomic license
- datomic-pro downloaded and unzipped

### Storage setup
1. Open a console in your OS of choise (Powershell in Windows for example or your favourite term in NIX).
1. CD into datomic-pro directory on your dev machine.
2. Copy the transactor properties file for DynamoDB:
`cp .\config\samples\ddb-transactor-template.properties my-transactor.properties`
3. Change your copy of the properties file (my-transactor.properties) to match your needs. The least amount of changes:
..* change the region name to be your preferred region;
..* change the db name to be "zots"
..* set the license key
4. Set environment variables for AWS credentials.
Powershell:
```shell
$env:AWS_ACCESS_KEY_ID = "your-access-key-id"
$env:AWS_SECRET_KEY = "your-secret-key"
```

Bash:
```bash
export AWS_ACCESS_KEY_ID="your-access-key-id"
export AWS_SECRET_KEY="your-secret-key"
```
5. Run datomic's script that will "ensure" the transactor's configuration. Warning: this will make changes in your AWS on your behalf. DynamoDB and S3 stuff will be created.
`.\bin\datomic ensure-transactor .\my-transactor.properties ensured-transactor.properties`

6. Try to run local transactor to see if everything's fine:
`.\bin\transactor .\ensured-transactor.properties`

### Transactor setup

1. Copy an example of a Cloud Formation config:
`cp .\config\samples\cf-template.properties my-cf.properties`

2. Change your copy of the config. Make sure you specify at least:
..* region name to match your preferred region
..* instace type (by default they have a quite expensive and big c3.large) at the time of writing the smallest one allowed was t2.small
..* allow connections from outside AWS for setting up and debugging from your machine: aws-ingress-cidrs=0.0.0.0/0 (you can disable internet access later from AWS console)

Feel free to disable monitoring, increase auto-scaling groups or change anything else.

3. Run datomic's script that will create an ensured version of your config:
`.\bin\datomic ensure-cf .\my-cf.properties ensured-cf.properties`

4. Run datomic's script that will create a CF template based on your ensured files:
`.\bin\datomic create-cf-template .\ensured-transactor.properties .\ensured-cf.properties cf.json`

5. Now you can create your CF stack:
`.\bin\datomic create-cf-stack ap-southeast-1 MyTransactor cf.json`

You specify the region, the EC2 instance's name and the CF config that you've created in the previous step.

This command will actually create stuff on your AWS:
..* EC2 instace that will run your Transactor
..* CloudFormation profile
..* auto-scaling group that will keep watching EC2 instance and start another one if needed (see AWS docs for more)

Wait for the instance to start. You can see it in your AWS console.

6. Run datomic shell on your machine and execute a command to create the database:
`.\bin\shell`

The URI should consiste of datomic:ddb://REGION/DynamoDB-NAME/DB-NAME?aws_key_id=&aws_secret_key=

Example of setting the URI:
`datomic %uri = "datomic:ddb://ap-southeast-1/zots/zots-db?aws_access_key_id=KEY_ID&aws_secret_key=SECRET";`

THis URI will also be in place of URI_TO_DB_TRANSACTOR in the Peer Server section.

Run create database command from the shell:
`Peer.createDatabase(uri);`

7. Run a peer server locally connecting to your new transactor to make sure it's working:
```powershell
bin/run -m datomic.peer-server -h localhost -p 8998 -a myaccesskey,mysecret -d test,datomic:ddb://ap-southeast-1/zots/zots-db?aws_access_key_id="&"aws_secret_key=
```

If you get no errors, your transactor is setup and running.

## Sequence or starting up the system

1. Storage (should be running if you setup AWS).
2. Transactor (should be running if you setup AWS).
3. Peer server.
4. Game server.


## Run Datomic Peer server

If you've followed the steps in Setup Datomic on AWS section you should already have Storage and Transactor running. You should also have a db name and transactor's URI noted somewhere.

1. Download datomic-pro version that you have a license for.
2. Unzip into a folder you prefer.
3. Generate a password that you will use to connect peer clients to this server. Think of a username that peers will use with this password. Use the database name that you've used to create the database.
4. Start the peer server using examples below.
Feel free to change the folder name, port, host and other configuration parameters of the command to suit your setup.

Example of the peer server that's running on localhost and connecting to a transactor running on AWS DynamoDB.

```bash
nohup ./datomic-pro-0.9.5697/bin/run
  -m datomic.peer-server -h localhost -p 8998
  -a zotsdbpeer,<GENERATED_PASSWORD_FOR_PEERS>
  -d zots-db,datomic:ddb://<URI_TO_DB_TRANSACTOR> &
```

Or if you want more control:
```bash
cd ./datomic-pro-0.9.5697/ &&
nohup java -server -Xmx400M -Xms400M
-cp resources:lib/*:datomic-transactor-pro-0.9.5697.jar:samples/clj:bin: clojure.main
-i bin/bridge.clj -m datomic.peer-server -h localhost -p 8998
-a <PEER_USERNAME>,<GENERATED_PASSWORD_FOR_PEERS>
-d <DB_NAME>,datomic:ddb://<URI_TO_DB_TRANSACTOR> &
```

## Run the Game server

1. Download zots.jar file to your server.
2. Make sure you have _resources/system.edn_ file in the same directory.
3. Setup all the env variables by running this bash code:
```bash
export AWS_ACCESS_KEY_ID="<AWS_ACCESS_KEY_ID>"
export AWS_SECRET_KEY="<AWS_SECRET_KEY>"
export ZOTS_DB_ACCESS_KEY="<PEER_USERNAME>"
export ZOTS_DB_SECRET="<GENERATED_PASSWORD_FOR_PEERS>"
export ZOTS_DB_ENDPOINT="localhost:8998"
export ZOTS_DB_NAME="<DB_NAME>"
```
4. Start the server using this command:
  `nohup java -jar zots.jar -Dclojure.spec.check-asserts=true -Xmx400m -server &`

Of course you should change the `Xmx` setting to your needs or to your machine's abilities.


## Example of pscp command to upload the Game

`pscp -i key.ppk /path/to/uberjar/zots.jar \zots.jar user-name@server-name:/path/to/game`
