package dev.pgm.community.network.updates.types;

import dev.pgm.community.assistance.AssistanceRequest;
import dev.pgm.community.network.Channels;
import dev.pgm.community.network.updates.NetworkUpdateBase;

/** AssistUpdate - Called when an {@link AssistanceRequest} is made */
public class AssistUpdate extends NetworkUpdateBase<AssistanceRequest> {

  public AssistUpdate(AssistanceRequest request) {
    super(request, Channels.ASSISTANCE);
  }
}
