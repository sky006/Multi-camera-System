/*
 * (C) Copyright 2014-2016 Kurento (http://kurento.org/)
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
 *
 */
package kurento;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.WebSocketSession;

/**
 * Map of users registered in the system. This class has a concurrent hash map to store users, using
 * its name as key in the map.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
public class UserRegistry {

  private ConcurrentHashMap<String, UserSession> usersBySessionIdAndNum = new ConcurrentHashMap<>();

  public void register(UserSession user) {
    usersBySessionIdAndNum.put(user.getId(), user);
  }

  public UserSession getById(String id) {
    return usersBySessionIdAndNum.get(id);
  }

  public UserSession getBySessionAndNum(WebSocketSession session, String num) {
    return usersBySessionIdAndNum.get(session.getId()+num);
  }

  public boolean exists(String id) {
    return usersBySessionIdAndNum.keySet().contains(id);
  }

  public UserSession removeBySession(WebSocketSession session, String num) {
    final UserSession user = getBySessionAndNum(session, num);
    if (user != null) {
      usersBySessionIdAndNum.remove(session.getId()+num);
    }
    return user;
  }

}
