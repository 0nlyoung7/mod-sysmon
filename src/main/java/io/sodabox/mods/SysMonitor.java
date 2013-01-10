package io.sodabox.mods;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.nhncorp.mods.socket.io.SocketIOServer;
import com.nhncorp.mods.socket.io.SocketIOSocket;

public class SysMonitor extends BusModBase implements Handler<Message<JsonObject>> {

	private Logger log;

	private String address;

	private String host;
	private int port;
	private String channel;
	private String type;

	private long timerId = -1;

	private Thread subThread;

	private final int DEFAULT_INTERVAL = 2000;
	RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
	MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

	private SocketIOServer ioServer;

	private JedisPool jedisPool;

	public void start() {
		super.start();

		address = getOptionalStringConfig("address", "sodabox.sysmon");
		host = getOptionalStringConfig("host", "localhost");
		port = getOptionalIntConfig("port", 8080);
		channel = getOptionalStringConfig("channel", "SYSMON");
		type = getOptionalStringConfig("type", "agent");

		new SubscribeThread(log, eb, address, host, port, channel);

		subThread = new Thread(
				new SubscribeThread(log, eb, address, host, port, channel)
				);

		subThread.start();

		JedisPoolConfig config = new JedisPoolConfig();
		config.testOnBorrow = true;


		if( host == null || host.length() == 0 ){
			jedisPool = new JedisPool(config, "localhost");
		}else{
			jedisPool = new JedisPool(config, host, port);
		}

		eb.registerHandler(address, this);
				
		// Server Start
		//if( type.equals( "master" ) ){
			SocketServer sockJs = new SocketServer();
			sockJs.init( vertx );
			ioServer = sockJs.getSocketIOServer();			
		//}
		// Server End
	}

	public void stop() {
		//subThread.stop();
	}

	public void handle(Message<JsonObject> message) {

		String action = message.body.getString("action");

		if (action == null) {
			sendError(message, "action must be specified");
			return;
		}

		Jedis jedis = jedisPool.getResource();
		String uniqueKey = getIpAddress();
		
		switch (action) {
		case "subscribe":
			timerId = vertx.setPeriodic(DEFAULT_INTERVAL, new Handler<Long>() {
				public void handle(Long event) {
					sendStats();
				}
			});
			
			JsonObject info = new JsonObject();
			info.putString("uniqueKey", uniqueKey );
			
			Map<String,String> map = jedis.hgetAll( "AGENT_LIST" );
			
			if( map == null || ! map.containsKey( uniqueKey ) ){
				jedis.hset( "AGENT_LIST", uniqueKey, info.toString() );
			}
			
			if( ioServer != null ){
				ioServer.sockets().onConnection(new Handler<SocketIOSocket>() {
		            public void handle(final SocketIOSocket socket) {
		            	JsonObject data = new JsonObject();
		            	
		            	Map<String,String> agentList = jedisPool.getResource().hgetAll( "AGENT_LIST" );
		            	Iterator<String> it = agentList.keySet().iterator();
		            	while( it.hasNext() ){
		            		String key = ( String )it.next();
		            		data.putString( key, agentList.get( key ) );
		            	}
		            	
		            	socket.emit( "addHost", data );
		            }
				});
			}
			
			break;
		case "unsubscribe":
			vertx.cancelTimer(timerId);	
			jedis.hdel( "AGENT_LIST", uniqueKey );
			break;
		case "message":
			ioServer.sockets().emit( "draw", message.body );
			//if( type.equals( "master" ) ){
				//ioServer.sockets().emit( "draw", message.body );
			//} else {
				//System.out.println( message.body );
			//}
			break;
		default:
			sendError(message, "Invalid action: " + action);
			return;
		}
		
		jedisPool.returnResource(jedis);
	}

	private void sendStats() {

		MemoryUsage usage = memoryMXBean.getHeapMemoryUsage();
		long maxMem = usage.getMax();
		long usedMem = usage.getUsed();
		float heapPercentageUsage = 100 * ((float) usedMem/maxMem);
		long uptime = runtimeMXBean.getUptime();

		JsonObject stats = new JsonObject()
		.putNumber("uptime", uptime)
		.putNumber("heapUsage", usedMem)
		.putNumber("heapPercentageUsage", heapPercentageUsage)
		.putString("hostName", getHostName())
		.putString("ipAddress", getIpAddress());

		Jedis jedis = jedisPool.getResource();
		jedis.publish(channel, stats.encode());
		jedisPool.returnResource(jedis);
	}
	
	public String getHostName() {
		String hostName = "";
		try{
			Runtime rt = java.lang.Runtime.getRuntime();
			Process proc = rt.exec("hostname");
			int inp;
			while ((inp = proc.getInputStream().read()) != -1) {
				hostName+=(char)inp;
			}
			proc.waitFor();
		   
		}catch(Exception e) {
			e.printStackTrace();
		}
		return hostName.trim();
	}
	
	public String getIpAddress(){
		String ipAddress = null;
		try{
			InetAddress Address = InetAddress.getLocalHost();
			ipAddress = Address.getHostAddress();
		} catch ( UnknownHostException e ){
			e.printStackTrace();
		}
        return ipAddress;
	}
}