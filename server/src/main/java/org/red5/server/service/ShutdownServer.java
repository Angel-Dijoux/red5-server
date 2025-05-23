/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.server.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.ContextLoader;
import org.red5.server.LoaderBase;
import org.red5.server.plugin.PluginRegistry;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Server/service to perform orderly and controlled shutdown and clean up of Red5.
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class ShutdownServer implements ApplicationContextAware, InitializingBean, DisposableBean {

  private Logger log = Red5LoggerFactory.getLogger(ShutdownServer.class);

  /** Port to which the server listens for shutdown requests. Default is 9999. */
  private int port = 9999;

  /** Delay or wait time in seconds before exiting. */
  private int shutdownDelay = 30;

  /** Name for the file containing the shutdown token */
  private String shutdownTokenFileName = "shutdown.token";

  /** Spring Application context */
  private ApplicationContext applicationContext;

  /** Red5 core context */
  private ApplicationContext coreContext;

  /** Red5 core context */
  private ApplicationContext commonContext;

  /** Red5 context loader */
  private ContextLoader contextLoader;

  // random token to verify shutdown request is genuine
  private final String token = UUID.randomUUID().toString();

  // whether the server is shutdown
  private AtomicBoolean shutdown = new AtomicBoolean(false);

  // thread executor for the internal startup and shutdown
  private ExecutorService executor =
      Executors.newFixedThreadPool(
          2,
          r -> {
            Thread t = new Thread(r, "ShutdownServer-" + r.hashCode());
            t.setDaemon(true);
            return t;
          });

  // reference to the runnable
  private Future<?> future;

  // reference to the jee server
  private LoaderBase jeeServer;

  @Override
  public void afterPropertiesSet() throws Exception {
    try {
      // check for an embedded jee server
      jeeServer = applicationContext.getBean(LoaderBase.class);
      // lookup the jee container
      if (jeeServer != null) {
        log.info("JEE server was found: {}", jeeServer.toString());
      }
    } catch (Exception e) {
    }
    // start blocks, so it must be on its own thread
    future =
        executor.submit(
            () -> {
              start();
            });
  }

  @Override
  public void destroy() throws Exception {
    shutdownOrderly();
    future.cancel(true);
  }

  /** Starts internal server listening for shutdown requests. */
  public void start() {
    log.info("Shutdown server start");
    // dump to stdout
    System.out.printf("Token: %s%n", token);
    // write out the token to a file so that red5 may be shutdown external to this VM instance.
    try {
      // delete existing file
      Files.deleteIfExists(Paths.get(shutdownTokenFileName));
      // write to file
      Path path = Files.createFile(Paths.get(shutdownTokenFileName));
      File tokenFile = path.toFile();
      RandomAccessFile raf = new RandomAccessFile(tokenFile, "rws");
      raf.write(token.getBytes());
      raf.close();
    } catch (Exception e) {
      log.warn("Exception handling token file", e);
    }
    final InetAddress loopbackAddr = InetAddress.getLoopbackAddress();
    // handle format of localhost/127.0.0.1
    final String localAddr = loopbackAddr.getHostAddress();
    int pos = localAddr.indexOf('/');
    final String localIPAddr = pos >= 0 ? localAddr.substring(pos) : localAddr;
    log.info("Starting socket server on {}:{}", localIPAddr, port);
    ServerSocket serverSocket = null;
    try {
      // server socket on loopback address (127.0.0.1) with given port and a backlog of 8
      serverSocket = new ServerSocket(port, 8, loopbackAddr);
      while (!shutdown.get()) {
        try {
          Socket clientSocket = serverSocket.accept();
          PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
          BufferedReader in =
              new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
          log.info(
              "Connected - local: {} remote: {}",
              clientSocket.getLocalSocketAddress(),
              clientSocket.getRemoteSocketAddress());
          String remoteAddr = clientSocket.getRemoteSocketAddress().toString();
          // handle SocketAddress format of /127.0.0.1:9999
          String remoteIPAddr = remoteAddr.substring(1, remoteAddr.indexOf(':'));
          log.info("IP addresses - local: {} remote: {}", localIPAddr, remoteIPAddr);
          if (localIPAddr.equals(remoteIPAddr)) {
            String inputLine = in.readLine();
            if (inputLine != null && token.equals(inputLine)) {
              log.info("Shutdown request validated using token");
              out.println("Ok");
              shutdownOrderly();
            } else {
              out.println("Invalid input");
            }
          } else {
            out.println("Invalid requester");
          }
          clientSocket.close();
        } catch (Throwable t) { // catch anything we might encounter
          log.warn(
              "Exception caught when trying to listen on port {} or listening for a connection",
              port,
              t);
        }
      }
    } catch (Exception be) {
      log.error(
          "Cannot bind to port: {}, ensure no other instances are bound or choose another port",
          port,
          be);
      shutdownOrderly();
    } finally {
      if (serverSocket != null) {
        try {
          serverSocket.close();
        } catch (IOException ioe) {
          log.warn("IOException at close", ioe);
        }
      }
    }
  }

  private void shutdownOrderly() {
    // shutdown internal listener
    if (shutdown.compareAndSet(false, true)) {
      log.info("Shutdown server shutdown");
      // shutdown the plug-in launcher
      try {
        log.debug("Attempting to shutdown plugin registry");
        PluginRegistry.shutdown();
      } catch (Exception e) {
        log.warn("Exception shutting down plugin registry", e);
      }
      // shutdown the context loader
      if (contextLoader != null) {
        log.debug("Attempting to shutdown context loader");
        contextLoader.shutdown();
        contextLoader = null;
      }
      // shutdown the jee server
      if (jeeServer != null) {
        // destroy is a DisposibleBean method not LoaderBase
        // jeeServer.destroy();
        jeeServer = null;
      }
      // attempt to kill the contexts
      final CountDownLatch latch = new CountDownLatch(3);
      executor.submit(
          () -> {
            try {
              log.debug("Attempting to close core context");
              ((ConfigurableApplicationContext) coreContext).close();
              latch.countDown();
            } catch (Exception e) {
              e.printStackTrace();
            }
          });
      executor.submit(
          () -> {
            try {
              log.debug("Attempting to close common context");
              ((ConfigurableApplicationContext) commonContext).close();
              latch.countDown();
            } catch (Exception e) {
              e.printStackTrace();
            }
          });
      executor.submit(
          () -> {
            try {
              log.debug("Attempting to close app context");
              ((ConfigurableApplicationContext) applicationContext).close();
              latch.countDown();
            } catch (Exception e) {
              e.printStackTrace();
            }
          });
      try {
        if (latch.await(shutdownDelay, TimeUnit.SECONDS)) {
          log.info("Application contexts are closed");
        } else {
          log.info("One or more contexts didn't close in the allotted time");
        }
      } catch (InterruptedException e) {
        log.error("Exception attempting to close app contexts", e);
      } finally {
        // shutdown the executor
        executor.shutdown();
      }
      // exit
      System.exit(0);
    }
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setShutdownDelay(int shutdownDelay) {
    this.shutdownDelay = shutdownDelay;
  }

  public void setShutdownTokenFileName(String shutdownTokenFileName) {
    this.shutdownTokenFileName = shutdownTokenFileName;
  }

  @SuppressWarnings("null")
  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  public void setCoreContext(ApplicationContext coreContext) {
    this.coreContext = coreContext;
  }

  public void setCommonContext(ApplicationContext commonContext) {
    this.commonContext = commonContext;
  }

  public void setContextLoader(ContextLoader contextLoader) {
    this.contextLoader = contextLoader;
  }
}
