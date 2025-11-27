/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.interezen.t24.redis_finder.receiver;

import ch.qos.logback.classic.Logger;
import com.interezen.api.cojlib.CoObject;
import com.interezen.api.cojlib.CoThread;
import com.interezen.api.cojlib.ILifeCycle;
import com.interezen.t24.redis_finder.cfg.StaticProperties;
import com.interezen.t24.redis_finder.logger.ReceiveLogger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.ragdoll.express.utils.ExpressExceptionUtils;

public class ReceiveServer extends CoObject implements ILifeCycle {

    private final int port;
    private int objectIndex = 0;
  	private CoThread thread = null;
  	private boolean bLoop = true;
 	private Logger logger = ReceiveLogger.getInstance().getLogger();

    public ReceiveServer(int port) {
        this.port = port;
        this.objectIndex = this.hashCode();
        this.setName((new StringBuilder())
				.append("ReceiveServer#")
				.append( this.hashCode() )
				.toString());
        thread = new CoThread( objectIndex, this );
    }

   	public void stop() {
   		this.bLoop = false;
   	}
       
       /** 쓰레드 생성/구동 매소드 */
   	public void start()	{
   		if( thread != null ) {
   			thread.start();
   		} 
   	}
   	
	//////////////////////////////////E V E N T  P R O C E S S  //////////////////////////////////
	@Override
	public void OnInit() {} 
	
	@Override
	public void OnBegin() {}
	
	@Override
	public void OnProcess() throws Exception {
		while(bLoop) {
			try {
				runLogic();
			} catch(Exception e) {
				logger.error("{} => {}", getName(), ExpressExceptionUtils.getStackTrace(e));
			}
			sleep(5000);
		}
	}
	
	@Override
   	public void OnDestroy(){}

    public void runLogic() throws Exception {
    	
    	logger.info("");
    	logger.info(" +========================================+");
		logger.info(" |         Redis Finder SERVER          |");
		logger.info(" +========================================+");

		int threadCount = StaticProperties.getInstance().getInt("receiver.worker.thread.count", 0);
		int backlog = StaticProperties.getInstance().getInt("receiver.backlog", 256);
		int linger = StaticProperties.getInstance().getInt("receiver.linger", -1);

        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup;

		if(threadCount > 0) {
			workerGroup = new NioEventLoopGroup(threadCount);
		} else {
			workerGroup = new NioEventLoopGroup();
		}

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_BACKLOG, backlog)
             .childOption(ChannelOption.SO_REUSEADDR, true)
             .childOption(ChannelOption.TCP_NODELAY, true);
			if(linger > -1) {
				b.childOption(ChannelOption.SO_LINGER, linger);
			}
			b.childHandler(new ReceiveInitializer());

            // Start the server.
            ChannelFuture f = b.bind(port).sync();
            
            logger.info("{} => Server started at port : {}",getName(), port);
            logger.info("{} => Options : {}", getName(), b.toString());
            logger.info("{} => Wait until the server socket is closed", getName());

            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            logger.info("{} => Shut down all event loops to terminate all threads", getName());
        }
    }
}
