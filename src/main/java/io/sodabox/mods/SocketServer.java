package io.sodabox.mods;

/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.deploy.Verticle;

import com.nhncorp.mods.socket.io.SocketIOServer;
import com.nhncorp.mods.socket.io.SocketIOSocket;
import com.nhncorp.mods.socket.io.impl.DefaultSocketIOServer;

public class SocketServer extends Verticle {
	
	private static final String WEB_ROOT = "static";
	
	private SocketIOServer io;
	
    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();
        
		server.requestHandler(new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				String filePath = WEB_ROOT;
				String requestPath = request.path;
				if(requestPath.equals("/")) {
					filePath += "/index.html";
				} else {
					filePath += requestPath; 
				}

				request.response.sendFile(filePath);
			}
		}); 
        server.listen(8080);	

		HttpServer socketServer = vertx.createHttpServer();
        io = new DefaultSocketIOServer(vertx, socketServer);
        io.sockets().onConnection(new Handler<SocketIOSocket>() {
            public void handle(final SocketIOSocket socket) {
            }
        });
        socketServer.listen(9090);
    }
	
	public void init( Vertx vertx ){
		this.vertx = vertx;
		start();
	}
	
	public SocketIOServer getSocketIOServer(){
		return io;
	}
}