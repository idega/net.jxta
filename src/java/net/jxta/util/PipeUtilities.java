package net.jxta.util;

import java.net.URI;
import net.jxta.pipe.*;
import net.jxta.id.*;
import net.jxta.peergroup.*;
import net.jxta.endpoint.*;
import net.jxta.document.*;
import net.jxta.protocol.*;
import net.jxta.exception.*;

public final class PipeUtilities {
    
    private PipeUtilities() {}
    
	public static PipeAdvertisement createPipeAdvertisement() {
		return (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
	}


	public static PipeAdvertisement createPipeAdvertisement(PipeID pipeId, String pipeType) {
		PipeAdvertisement pipeAdvertisement = createPipeAdvertisement();
		pipeAdvertisement.setPipeID(pipeId);
		pipeAdvertisement.setType(pipeType);
		return pipeAdvertisement;
	}

	
	public static PipeAdvertisement createPipeAdvertisement(String pipeIdText, String pipeType) throws JxtaException {
		PipeID pipeId = (PipeID) ID.create( URI.create(pipeIdText));
		PipeAdvertisement pipeAdvertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
		pipeAdvertisement.setPipeID(pipeId);
		pipeAdvertisement.setType(pipeType);
		return pipeAdvertisement;
	}
	
	public static PipeAdvertisement createPipeAdvertisement(Element root) {
		TextElement pipeAdvElement = (TextElement) DocumentUtilities.getChild(root, PipeAdvertisement.getAdvertisementType());
		
		if (pipeAdvElement == null) {
			return null;
        }
			
		return (PipeAdvertisement) AdvertisementFactory.newAdvertisement(pipeAdvElement);
	}

	public static PipeAdvertisement createNewPipeAdvertisement(PeerGroup peerGroup, String pipeType) {
		PipeAdvertisement pipeAdvertisement = createPipeAdvertisement();
		
        PipeID pipeID = IDFactory.newPipeID((PeerGroupID) peerGroup.getPeerGroupID());
		
		pipeAdvertisement.setPipeID(pipeID);
		pipeAdvertisement.setType(pipeType);
		return pipeAdvertisement;
	}

	
}
