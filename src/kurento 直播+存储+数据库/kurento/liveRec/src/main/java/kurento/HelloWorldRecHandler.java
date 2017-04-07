/*
 * (C) Copyright 2015-2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kurento;

import java.io.IOException;
import java.util.Date;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.sql.*;

import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.MediaType;
import org.kurento.client.PausedEvent;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.RecordingEvent;
import org.kurento.client.StoppedEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Hello World with recording handler (application and media logic).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 6.1.1
 */
public class HelloWorldRecHandler extends TextWebSocketHandler {

	private final Logger log = LoggerFactory.getLogger(HelloWorldRecHandler.class);
	private static final Gson gson = new GsonBuilder().create();

	@Autowired
	private UserRegistry registry;

	@Autowired
	private KurentoClient kurento;

	public Timer timer = null;
	public int seconds = 300; //各段存储时长
	
	private Connection con=null ;
	private Statement sql;
	private static final String dbURL = "jdbc:mysql://localhost:3306/multi_track"; 
	private static final String dbUser = "liveRec"; //数据库用户名
	private static final String dbPwd = "a12345678";//数据库密码

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

		log.debug("Incoming message: {}", jsonMessage);

		UserSession user = registry.getBySessionAndNum(session, jsonMessage.get("num").getAsString());
		if (user != null) {
			log.debug("Incoming message from user '{}': {}", user.getId(), jsonMessage);
		} else {
			log.debug("Incoming message from new user: {}", jsonMessage);
		}

		switch (jsonMessage.get("id").getAsString()) {
		case "start":
			try {  
	            Class.forName("com.mysql.jdbc.Driver") ;  
	        } catch (ClassNotFoundException e1) {  
	            e1.printStackTrace();  
	        }
			try {
				con =DriverManager.getConnection(dbURL, dbUser, dbPwd);
				con.setAutoCommit(true);
				sql=con.createStatement();
				System.out.println("Success!!!");
			}catch(SQLException e) {
				try{
					con.rollback();
	            }
	            catch(SQLException exp){}
				e.printStackTrace();
			}
			timer = new Timer();  
			start(session, jsonMessage);
			break;
		case "stop":
			timer.cancel();
			if (user != null) {
				user.stop();
				user.release();
			}
			break;
		case "onIceCandidate": {
			JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();

			if (user != null) {
				IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(),
						jsonCandidate.get("sdpMid").getAsString(), jsonCandidate.get("sdpMLineIndex").getAsInt());
				user.addCandidate(candidate);
			}
			break;
		}
		default:
			sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
			break;
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		super.afterConnectionClosed(session, status);
		registry.removeBySession(session, "1");
		registry.removeBySession(session, "2");
		registry.removeBySession(session, "3");
		registry.removeBySession(session, "4");
		registry.removeBySession(session, "5");
		registry.removeBySession(session, "6");
	}

	private void start(final WebSocketSession session, JsonObject jsonMessage) {
		try {

			final String num = jsonMessage.get("num").getAsString();

			// 1. Media logic
			final UserSession user = new UserSession(session, num);
			final MediaPipeline pipeline = kurento.createMediaPipeline();
			user.setMediaPipeline(pipeline);
			final WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
			user.setWebRtcEndpoint(webRtcEndpoint);
			String videourl = jsonMessage.get("videourl").getAsString();
			final PlayerEndpoint playerEndpoint = new PlayerEndpoint.Builder(pipeline, videourl).build();
			user.setPlayerEndpoint(playerEndpoint);
			playerEndpoint.connect(webRtcEndpoint);

			playerEndpoint.addErrorListener(new EventListener<ErrorEvent>() {
				@Override
				public void onEvent(ErrorEvent event) {
					log.info("ErrorEvent: {}", event.getDescription());
					sendPlayEnd(session, pipeline, num);
				}
			});

			playerEndpoint.play();

			// 2. SDP negotiation
			String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
			String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

			// 3. Gather ICE candidates
			webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

				@Override
				public void onEvent(IceCandidateFoundEvent event) {
					JsonObject response = new JsonObject();
					response.addProperty("id", "iceCandidate");
					response.addProperty("num", num);
					response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
					try {
						synchronized (session) {
							session.sendMessage(new TextMessage(response.toString()));
						}
					} catch (IOException e) {
						log.error(e.getMessage());
					}
				}
			});

			final JsonObject response = new JsonObject();
			response.addProperty("id", "startResponse");
			response.addProperty("num", num);
			response.addProperty("sdpAnswer", sdpAnswer);

			webRtcEndpoint.gatherCandidates();

			// ---------------recorcer-------------
			final SimpleDateFormat recpath = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
			final SimpleDateFormat rectime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date now = new Date();
			Calendar cld = Calendar.getInstance();
		    cld.add(Calendar.SECOND, seconds);
		    
			final String RECORDER_FILE_PATH = jsonMessage.get("recpath").getAsString();
			RecorderEndpoint recorder = new RecorderEndpoint.Builder(pipeline,
					RECORDER_FILE_PATH + recpath.format(now).toString() + ".mp4").build();
			
			sql.executeUpdate("INSERT INTO camera"+String.valueOf(num)
					+" VALUES("+String.valueOf(now.getTime()/1000)+",'"+rectime.format(now).toString()
					+"','"+rectime.format(cld.getTime()).toString()+"','"
					+RECORDER_FILE_PATH + recpath.format(now).toString() + ".mp4'"+")");

			recorder.addRecordingListener(new EventListener<RecordingEvent>() {

				@Override
				public void onEvent(RecordingEvent event) {
					JsonObject response = new JsonObject();
					response.addProperty("id", "recording");
					response.addProperty("num", num);
					try {
						synchronized (session) {
							session.sendMessage(new TextMessage(response.toString()));
						}
					} catch (IOException e) {
						log.error(e.getMessage());
					}
				}

			});

			recorder.addStoppedListener(new EventListener<StoppedEvent>() {

				@Override
				public void onEvent(StoppedEvent event) {
					JsonObject response = new JsonObject();
					response.addProperty("id", "stopped");
					response.addProperty("num", num);
					try {
						synchronized (session) {
							session.sendMessage(new TextMessage(response.toString()));
						}
					} catch (IOException e) {
						log.error(e.getMessage());
					}
				}

			});

			recorder.addPausedListener(new EventListener<PausedEvent>() {

				@Override
				public void onEvent(PausedEvent event) {
					JsonObject response = new JsonObject();
					response.addProperty("id", "paused");
					response.addProperty("num", num);
					try {
						synchronized (session) {
							session.sendMessage(new TextMessage(response.toString()));
						}
					} catch (IOException e) {
						log.error(e.getMessage());
					}
				}

			});

			playerEndpoint.connect(recorder, MediaType.AUDIO);
			playerEndpoint.connect(recorder, MediaType.VIDEO);

			// Store user session
			user.setRecorderEndpoint(recorder);
			registry.register(user);

			synchronized (user) {
				session.sendMessage(new TextMessage(response.toString()));
			}

			webRtcEndpoint.gatherCandidates();

			recorder.record();
			class Task extends TimerTask {
				public RecorderEndpoint recorder;
				public Date date;
				public Task(RecorderEndpoint recorder) {
					this.recorder=recorder;
				}
				public void run() {
					try {
						recorder.stop();
						/*
						 * if (recorder != null) { final CountDownLatch
						 * stoppedCountDown = new CountDownLatch(1);
						 * ListenerSubscription subscriptionId = recorder
						 * .addStoppedListener(new EventListener<StoppedEvent>() {
						 * 
						 * @Override public void onEvent(StoppedEvent event) {
						 * stoppedCountDown.countDown(); } }); recorder.stop(); try
						 * { if (!stoppedCountDown.await(5, TimeUnit.SECONDS)) {
						 * log.error("Error waiting for recorder to stop"); } }
						 * catch (InterruptedException e) {
						 * log.error("Exception while waiting for state change", e);
						 * } recorder.removeStoppedListener(subscriptionId); }
						 */
						date = new Date();
						Calendar cal = Calendar.getInstance();
					    cal.add(Calendar.SECOND, seconds);
						recorder = new RecorderEndpoint.Builder(pipeline,
								RECORDER_FILE_PATH + recpath.format(date).toString() + ".mp4").build();
						
						sql.executeUpdate("INSERT INTO camera"+String.valueOf(num)
						+" VALUES("+String.valueOf(date.getTime()/1000)+",'"+rectime.format(date).toString()
						+"','"+rectime.format(cal.getTime()).toString()+"','"
						+RECORDER_FILE_PATH + recpath.format(date).toString() + ".mp4'"+")");
						
						recorder.addRecordingListener(new EventListener<RecordingEvent>() {

							@Override
							public void onEvent(RecordingEvent event) {
								JsonObject response = new JsonObject();
								response.addProperty("id", "recording");
								response.addProperty("num", num);
								try {
									synchronized (session) {
										session.sendMessage(new TextMessage(response.toString()));
									}
								} catch (IOException e) {
									log.error(e.getMessage());
								}
							}

						});

						recorder.addStoppedListener(new EventListener<StoppedEvent>() {

							@Override
							public void onEvent(StoppedEvent event) {
								JsonObject response = new JsonObject();
								response.addProperty("id", "stopped");
								response.addProperty("num", num);
								try {
									synchronized (session) {
										session.sendMessage(new TextMessage(response.toString()));
									}
								} catch (IOException e) {
									log.error(e.getMessage());
								}
							}

						});

						recorder.addPausedListener(new EventListener<PausedEvent>() {

							@Override
							public void onEvent(PausedEvent event) {
								JsonObject response = new JsonObject();
								response.addProperty("id", "paused");
								response.addProperty("num", num);
								try {
									synchronized (session) {
										session.sendMessage(new TextMessage(response.toString()));
									}
								} catch (IOException e) {
									log.error(e.getMessage());
								}
							}

						});

						playerEndpoint.connect(recorder, MediaType.AUDIO);
						playerEndpoint.connect(recorder, MediaType.VIDEO);

						// Store user session
						user.setRecorderEndpoint(recorder);
						registry.register(user);

						try {
							synchronized (user) {
								session.sendMessage(new TextMessage(response.toString()));
							}
						} catch (IOException e) {
							log.error(e.getMessage());
						}

						recorder.record();
						Timer end = new Timer();
						class RemindTask extends TimerTask {
							public RecorderEndpoint rec;
							public RemindTask(RecorderEndpoint rec) {
								this.rec=rec;
							}
							public void run () {
								rec.stop();
							}
						}
						end.schedule(new RemindTask(recorder), seconds*1000);
					} catch (Throwable t) {
						log.error("Start error", t);
						sendError(session, t.getMessage());
					}
				}
			}
			timer.schedule(new Task(recorder), seconds * 1000, seconds * 1000);


		} catch (Throwable t) {
			log.error("Start error", t);
			sendError(session, t.getMessage());
		}
	}

	public void sendPlayEnd(WebSocketSession session, MediaPipeline pipeline, String num) {
		try {
			JsonObject response = new JsonObject();
			response.addProperty("id", "playEnd");
			response.addProperty("num", num);
			session.sendMessage(new TextMessage(response.toString()));
		} catch (IOException e) {
			log.error("Error sending playEndOfStream message", e);
		}
		// Release pipeline
		pipeline.release();
	}

	private void sendError(WebSocketSession session, String message) {
		try {
			JsonObject response = new JsonObject();
			response.addProperty("id", "error");
			response.addProperty("message", message);
			session.sendMessage(new TextMessage(response.toString()));
		} catch (IOException e) {
			log.error("Exception sending message", e);
		}
	}
}
