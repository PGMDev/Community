package dev.pgm.community.network.subs.types;

import dev.pgm.community.assistance.AssistanceRequest;
import dev.pgm.community.assistance.feature.AssistanceFeature;
import dev.pgm.community.network.Channels;
import dev.pgm.community.network.subs.NetworkSubscriber;
import java.util.logging.Logger;

/** AssistanceSubscriber - Listens for {@link AssistanceRequest} */
public class AssistanceSubscriber extends NetworkSubscriber {

  private AssistanceFeature assist;

  public AssistanceSubscriber(AssistanceFeature assist, String networkId, Logger logger) {
    super(Channels.ASSISTANCE, networkId, logger);
    this.assist = assist;
  }

  @Override
  public void onReceiveUpdate(String data) {
    AssistanceRequest request = gson.fromJson(data, AssistanceRequest.class);
    if (request != null) {
      assist.recieveUpdate(request);
    }
  }
}
