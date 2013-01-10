package io.sodabox.mods;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class SubscribeThread implements Runnable {

	protected EventBus eb;

	private String address;
	
	private String host;
	private int port;
	private String channel;

	private Jedis 	jedis;

	private Logger log;

	public SubscribeThread(EventBus eb, String address, String host, int port, String channel){
		this(null, eb, address, host, port, channel);
	}

	public SubscribeThread(Logger log, EventBus eb, String address, String host, int port, String channel){
		this.log = log;
		this.eb = eb;
		
		this.address = address;
		this.channel = channel;

		this.host = host;
		this.port = port;

	}


	public void run() {

		if( host == null || host.length() == 0 ){
			this.jedis = new Jedis("localhost");
		}else{
			this.jedis = new Jedis(host, port);
		}

		LogUtils.DEBUG(log, "connected %s",jedis.ping());

		jedis.subscribe( new JedisPubSub() {

			@Override
			public void onMessage(String channel, String message) {
				LogUtils.DEBUG(log, "message (channel:%s)- %s", channel, message);
				
				JsonObject json = new JsonObject(message);
				json.putString("action", "message");
				eb.send(address, json);
			}

			@Override
			public void onSubscribe(String channel, int subscribedChannels) {

				JsonObject json = new JsonObject()
				.putString("channel", channel)
				.putString("action", "subscribe");
				
				eb.send(address, json);
			}

			@Override
			public void onUnsubscribe(String channel, int subscribedChannels) {

				JsonObject json = new JsonObject()
				.putString("channel", channel)
				.putString("action", "unsubscribe");

				eb.send(address, json);

			}

			@Override
			public void onPMessage(String pattern, String channel,String message) {
			}

			@Override
			public void onPUnsubscribe(String pattern, int subscribedChannels) {
			}

			@Override
			public void onPSubscribe(String pattern, int subscribedChannels) {

			}

		}, 
		channel);		
	}
}