
# is working project !!! do not using now.. :)

# SysMon for vertx instances

This module allows SO/JVM status to be saved, retrieved in a monitoring Server. 

This is a worker module and must be started as a worker verticle and only one instance.

## Dependencies

This module requires a Redis server to be available on the network.

## Name

The module name is `sodabox-sysmon`.

## Configuration

The sodabox-sysmon module takes the following configuration for Agent:

    {
        "type": "agent"
        "address": <address>,
        "host": <host>,
        "port": <port>,
        "channel": <channel>    
    }
    
for Monitoring Server:

    {
        "type": "server"
        "address": <address>,
        "host": <host>,
        "port": <port>,
        "channel": <channel>    
    }    
    
    
