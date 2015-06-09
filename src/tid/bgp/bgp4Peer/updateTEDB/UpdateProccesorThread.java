package tid.bgp.bgp4Peer.updateTEDB;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import es.tid.bgp.bgp4.messages.BGP4Update;
import es.tid.bgp.bgp4.update.fields.LinkNLRI;
import es.tid.bgp.bgp4.update.fields.LinkStateNLRI;
import es.tid.bgp.bgp4.update.fields.NLRITypes;
import es.tid.bgp.bgp4.update.fields.NodeNLRI;
import es.tid.bgp.bgp4.update.fields.PathAttribute;
import es.tid.bgp.bgp4.update.fields.PrefixNLRI;
import es.tid.bgp.bgp4.update.fields.pathAttributes.AFICodes;
import es.tid.bgp.bgp4.update.fields.pathAttributes.BGP_LS_MP_Reach_Attribute;
import es.tid.bgp.bgp4.update.fields.pathAttributes.LinkStateAttribute;
import es.tid.bgp.bgp4.update.fields.pathAttributes.MP_Reach_Attribute;
import es.tid.bgp.bgp4.update.fields.pathAttributes.PathAttributesTypeCode;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.AdministrativeGroupLinkAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.DefaultTEMetricLinkAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.IGPFlagBitsPrefixAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.IPv4RouterIDLocalNodeLinkAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.IPv4RouterIDRemoteNodeLinkAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.IS_IS_AreaIdentifierNodeAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.LinkProtectionTypeLinkAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.MaxReservableBandwidthLinkAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.MaximumLinkBandwidthLinkAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.MetricLinkAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.NodeFlagBitsNodeAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.NodeNameNodeAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.OSPFForwardingAddressPrefixAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.PrefixMetricPrefixAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.RouteTagPrefixAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.SidLabelNodeAttribTLV;
import es.tid.bgp.bgp4.update.tlv.linkstate_attribute_tlvs.UnreservedBandwidthLinkAttribTLV;
import es.tid.bgp.bgp4.update.tlv.node_link_prefix_descriptor_subTLVs.AreaIDNodeDescriptorSubTLV;
import es.tid.bgp.bgp4.update.tlv.node_link_prefix_descriptor_subTLVs.AutonomousSystemNodeDescriptorSubTLV;
import es.tid.bgp.bgp4.update.tlv.node_link_prefix_descriptor_subTLVs.BGPLSIdentifierNodeDescriptorSubTLV;
import es.tid.bgp.bgp4.update.tlv.node_link_prefix_descriptor_subTLVs.IGPRouterIDNodeDescriptorSubTLV;
import es.tid.bgp.bgp4.update.tlv.node_link_prefix_descriptor_subTLVs.NodeDescriptorsSubTLV;
import es.tid.bgp.bgp4.update.tlv.node_link_prefix_descriptor_subTLVs.NodeDescriptorsSubTLVTypes;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.AdministrativeGroup;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.AvailableLabels;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.LinkProtectionType;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.MaximumBandwidth;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.MaximumReservableBandwidth;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.TrafficEngineeringMetric;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.UnreservedBandwidth;
import es.tid.ospf.ospfv2.lsa.tlv.subtlv.complexFields.BitmapLabelSet;
import tid.pce.tedb.SSONInformation;
import tid.pce.tedb.InterDomainEdge;
import tid.pce.tedb.IntraDomainEdge;
import tid.pce.tedb.MultiDomainTEDB;
import tid.pce.tedb.Node_Info;
import tid.pce.tedb.SimpleTEDB;
import tid.pce.tedb.TE_Information;
import tid.pce.tedb.WSONInformation;
import tid.bgp.bgp4Peer.tedb.IntraTEDBS;
/**
 * This class process the update messages updating the TEDB.
 * 
 *  WARNING: it is suppose to be a SimpleTEDB!!! It is not finished yet.
 * @author pac
 *
 */
public class UpdateProccesorThread extends Thread {
	/**
	 * Parameter to run the class if it is true
	 */
	private boolean running;
	/**
	 * Queue which stores the BGP4 update messages to be read and process
	 */
	private LinkedBlockingQueue<BGP4Update> updateList;

	/** LINK ATTRIBUTE TLVs */
	MaximumLinkBandwidthLinkAttribTLV maximumLinkBandwidthTLV= new MaximumLinkBandwidthLinkAttribTLV();
	MaxReservableBandwidthLinkAttribTLV maxReservableBandwidthTLV= new MaxReservableBandwidthLinkAttribTLV();
	UnreservedBandwidthLinkAttribTLV unreservedBandwidthTLV= new UnreservedBandwidthLinkAttribTLV();
	AdministrativeGroupLinkAttribTLV administrativeGroupTLV = new AdministrativeGroupLinkAttribTLV();
	LinkProtectionTypeLinkAttribTLV linkProtectionTLV = new LinkProtectionTypeLinkAttribTLV();
	MetricLinkAttribTLV metricTLV = new MetricLinkAttribTLV();
	IPv4RouterIDLocalNodeLinkAttribTLV iPv4RouterIDLocalNodeLATLV = new IPv4RouterIDLocalNodeLinkAttribTLV();
	IPv4RouterIDRemoteNodeLinkAttribTLV iPv4RouterIDRemoteNodeLATLV = new IPv4RouterIDRemoteNodeLinkAttribTLV();
	DefaultTEMetricLinkAttribTLV TEMetricTLV = new DefaultTEMetricLinkAttribTLV();

	/** NODE ATTRIBUTE TLVs 
	 * Ipv4 of local node link attribute TLV also used
	 * 
	 * */
	NodeFlagBitsNodeAttribTLV nodeFlagBitsTLV = new NodeFlagBitsNodeAttribTLV();
	NodeNameNodeAttribTLV nodeNameTLV = new NodeNameNodeAttribTLV();
	IS_IS_AreaIdentifierNodeAttribTLV areaIDTLV = new IS_IS_AreaIdentifierNodeAttribTLV();
	SidLabelNodeAttribTLV sidTLV = new SidLabelNodeAttribTLV();

	/**PREFIX ATTRIBUTE TLVs */
	IGPFlagBitsPrefixAttribTLV igpFlagBitsTLV = new IGPFlagBitsPrefixAttribTLV();
	RouteTagPrefixAttribTLV routeTagTLV = new RouteTagPrefixAttribTLV();
	PrefixMetricPrefixAttribTLV prefixMetricTLV = new PrefixMetricPrefixAttribTLV();
	OSPFForwardingAddressPrefixAttribTLV OSPFForwardingAddrTLV = new OSPFForwardingAddressPrefixAttribTLV();

	private AvailableLabels availableLabels;
	/**
	 * Logger
	 */
	private Logger log;
	/**
	 * Topology database for interDomain Links which will be updated.
	 */
	private MultiDomainTEDB multiTedb;
	/**
	 * Topology database for intradomain Links. It owns several domains and.
	 */
	private IntraTEDBS intraTEDB;

	private LinkedList<UpdateLink> updateLinks;

	private SimpleTEDB simpleTEDB;
	private TE_Information te_info;



	//Coleccion de mensajes update

	public UpdateProccesorThread(LinkedBlockingQueue<BGP4Update> updateList,MultiDomainTEDB multiTedb ,SimpleTEDB simpleTEDB /*, IntraTEDBS intraTEDB*/){
		running=true;
		this.updateList=updateList;
		this.multiTedb = multiTedb;
		log=Logger.getLogger("BGP4Server");
		log.info("In constructor::: simpleTEDB::"+simpleTEDB);
		this.simpleTEDB=simpleTEDB;
		this.availableLabels= new AvailableLabels();
		this.updateLinks=new LinkedList<UpdateLink>();
	}
	public void run(){	
		BGP4Update updateMsg;
		while (running) {
			try {
				PathAttribute att_ls = null;
				PathAttribute att_mpreach  = null; 
				PathAttribute att = null;
				updateMsg= updateList.take();
				log.finest("Update Procesor Thread Reading the message: \n"+ updateMsg.toString());	
				String learntFrom = updateMsg.getLearntFrom();
				ArrayList<PathAttribute> pathAttributeList = updateMsg.getPathAttributes();
				ArrayList<PathAttribute> pathAttributeListUtil = new ArrayList<PathAttribute>();			

				// buscamos los dos atributos que nos interesan...
				for (int i=0;i<pathAttributeList.size();i++){
					att = pathAttributeList.get(i);
					int typeCode = att.getTypeCode();
					switch (typeCode){
					case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_BGP_LS_ATTRIBUTE:
						att_ls = att;
						break;
					case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_MP_REACH_NLRI:
						att_mpreach = att;
						break;
					case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_ASPATH:
						//log.info("We don't use ASPATH");
						break;	
					case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_ORIGIN:
						//log.info("We don't use ORIGIN");
						break;	
					default:
						//log.info("Attribute typecode " + typeCode +"unknown");
						break;
					}

				}	

				//los situamos en el orden correcto para nuestra beloved ted...
				if(att_ls!=null)
					pathAttributeListUtil.add(att_ls);
				if(att_mpreach!=null)
					pathAttributeListUtil.add(att_mpreach);

				if (pathAttributeListUtil != null){
					for (int i=0;i<pathAttributeListUtil.size();i++){
						att = pathAttributeListUtil.get(i);
						int typeCode = att.getTypeCode();
						switch (typeCode){	
						// cuando encontramos el link state attribute rellenamos las tlvs que nos llegan para luego
						// meterlas en la te_info o en la node_info
						case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_BGP_LS_ATTRIBUTE:
							processAttributeLinkState((LinkStateAttribute) att);
							continue;
							// cuando procesamos el mp_reach distinguimos entre nodo y link...
							// prefijo aun por hacer
						case PathAttributesTypeCode.PATH_ATTRIBUTE_TYPECODE_MP_REACH_NLRI:
							int afi;
							afi = ((MP_Reach_Attribute)att).getAddressFamilyIdentifier();
							if (afi == AFICodes.AFI_BGP_LS){
								LinkStateNLRI nlri = (LinkStateNLRI) ((BGP_LS_MP_Reach_Attribute)att).getLsNLRI();
								int nlriType =  nlri.getNLRIType();
								switch (nlriType){					
								case NLRITypes.Link_NLRI:
									processLinkNLRI((LinkNLRI)(nlri), learntFrom);					
									continue;
								case NLRITypes.Node_NLRI:
									fillNodeInformation((NodeNLRI)(nlri), learntFrom);
									continue;
								case NLRITypes.Prefix_v4_NLRI://POR HACER...
									fillPrefixNLRI((PrefixNLRI)nlri, igpFlagBitsTLV, OSPFForwardingAddrTLV, prefixMetricTLV, routeTagTLV);
									continue;
								default:
									log.finest("Attribute Code unknown");
								}
							}
							continue;
						default:
							log.finest("Attribute Code unknown");
						}
					}
				}



			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	private void fillPrefixNLRI(PrefixNLRI nlri, IGPFlagBitsPrefixAttribTLV igpFlagBitsTLV, OSPFForwardingAddressPrefixAttribTLV oSPFForwardingAddrTLV, PrefixMetricPrefixAttribTLV prefixMetricTLV, RouteTagPrefixAttribTLV routeTagTLV) {
		// TODO Auto-generated method stub

	}
	/**
	 * Function which process the attribute link State. It updates the fields passed by argument. 
	 * @param lsAtt
	 * @param maximumLinkBandwidthTLV
	 * @param maxReservableBandwidthTLV
	 * @param unreservedBandwidthTLV
	 * @param nodeNameTLV 
	 * @param nodeFlagBitsTLV 
	 * @param tEMetricTLV 
	 * @param iPv4RouterIDRemoteNodeLATLV 
	 * @param iPv4RouterIDLocalNodeLATLV 
	 * @param metricTLV 
	 * @param linkProtectionTLV 
	 * @param administrativeGroupTLV 
	 * @param areaIDTLV 
	 * @param oSPFRouteTypeTLV 
	 * @param iPReachabiltyTLV 
	 * @param availableLabels
	 */
	private void processAttributeLinkState(LinkStateAttribute lsAtt){

		if (lsAtt.getMaximumLinkBandwidthTLV() != null){
			maximumLinkBandwidthTLV = lsAtt.getMaximumLinkBandwidthTLV();
		}

		if (lsAtt.getMaxReservableBandwidthTLV() != null){
			maxReservableBandwidthTLV = lsAtt.getMaxReservableBandwidthTLV();
		}
		if (lsAtt.getUnreservedBandwidthTLV() != null){
			unreservedBandwidthTLV = lsAtt.getUnreservedBandwidthTLV();
		}
		if(lsAtt.getAdministrativeGroupTLV() != null){
			administrativeGroupTLV = lsAtt.getAdministrativeGroupTLV();
		}
		if(lsAtt.getLinkProtectionTLV() != null){
			linkProtectionTLV = lsAtt.getLinkProtectionTLV();
		}
		if(lsAtt.getIPv4RouterIDLocalNodeLATLV()!= null){
			iPv4RouterIDLocalNodeLATLV = lsAtt.getIPv4RouterIDLocalNodeLATLV();
		}
		if(lsAtt.getIPv4RouterIDRemoteNodeLATLV()!=null){
			iPv4RouterIDRemoteNodeLATLV = lsAtt.getIPv4RouterIDRemoteNodeLATLV();
		}
		if(lsAtt.getMetricTLV() != null){
			metricTLV = lsAtt.getMetricTLV();
		}
		if(lsAtt.getTEMetricTLV()!=null){
			TEMetricTLV = lsAtt.getTEMetricTLV();
		}
		if(lsAtt.getNodeFlagBitsTLV()!= null){
			nodeFlagBitsTLV = lsAtt.getNodeFlagBitsTLV();
		}
		if(lsAtt.getNodeNameTLV() != null){
			nodeNameTLV = lsAtt.getNodeNameTLV();
		}
		if(lsAtt.getAreaIDTLV() != null){
			areaIDTLV = lsAtt.getAreaIDTLV();
		}
		if(lsAtt.getIgpFlagBitsTLV() != null){
			igpFlagBitsTLV= lsAtt.getIgpFlagBitsTLV();
		}
		if(lsAtt.getRouteTagTLV() != null){
			routeTagTLV = lsAtt.getRouteTagTLV();
		}
		if(lsAtt.getOSPFForwardingAddrTLV() != null){
			OSPFForwardingAddrTLV = lsAtt.getOSPFForwardingAddrTLV();
		}
		if(lsAtt.getSidLabelTLV()!=null){
			sidTLV = lsAtt.getSidLabelTLV();
		}
		/**if(lsAtt.getIPv4RouterIDLocalNodeNATLV()!=null){
			IPv4RouterIDLocalNodeNodeAttribTLV = lsAtt.getIPv4RouterIDLocalNodeNATLV();
			log.info("foundddddddddd16");
		}*/
		/**if(lsAtt.getIPv4RouterIDLocalNodeNATLV()!=null)
			IPv4RouterIDLocalNodeNodeAttribTLV = lsAtt.getIPv4RouterIDLocalNodeNATLV();*/
		if (lsAtt.getAvailableLabels() != null){
			this.availableLabels =lsAtt.getAvailableLabels();
		}

	}
	/**
	 * Function which process the link NLRI. It updates the fields passed by argument.
	 * @param linkNLRI
	 * @param maximumLinkBandwidthTLV
	 * @param maxReservableBandwidthTLV
	 * @param unreservedBandwidthTLV
	 * @param availableLabels
	 */


	/** Procesar un link significa:
	 * crear los vertices si no existen ya
	 * crear la edge si no existe ya
	 * crear la te_info o actualizarla
	 * @param linkNLRI
	 * @param learntFrom 
	 */
	private void processLinkNLRI(LinkNLRI linkNLRI, String learntFrom){
		ArrayList<NodeDescriptorsSubTLV> nodeDescriptorsSubTLV;
		//Dominios
		Inet4Address localDomainID= null ;
		Inet4Address remoteDomainID = null ;

		Inet4Address areaID= null ;
		Inet4Address bgplsID = null;

		Inet4Address LocalNodeIGPId = null;
		Inet4Address RemoteNodeIGPId = null;

		//Local Node descriptors 
		nodeDescriptorsSubTLV = linkNLRI.getLocalNodeDescriptors().getNodeDescriptorsSubTLVList();

		//no haria falta este for pero bueno, lo dejamos como recuerdo de @mcs
		for (int i = 0;i<nodeDescriptorsSubTLV.size();i++){
			int subTLVType = nodeDescriptorsSubTLV.get(i).getSubTLVType();
			switch (subTLVType){	
			case NodeDescriptorsSubTLVTypes.NODE_DESCRIPTORS_SUBTLV_TYPE_AUTONOMOUS_SYSTEM:
				localDomainID = ((AutonomousSystemNodeDescriptorSubTLV) nodeDescriptorsSubTLV.get(i)).getAS_ID();
				//log.info("AUTONOMOUS_SYSTEM found in LINK_NLRI(local_node). as_local "+localDomainID);
				continue;

			case NodeDescriptorsSubTLVTypes.NODE_DESCRIPTORS_SUBTLV_TYPE_AREA_ID:
				areaID = ((AreaIDNodeDescriptorSubTLV) nodeDescriptorsSubTLV.get(i)).getAREA_ID();
				//log.info("AREA_ID found in LINK_NLRI(local_node). area_id "+areaID);
				continue;

			case NodeDescriptorsSubTLVTypes.NODE_DESCRIPTORS_SUBTLV_TYPE_BGP_LS_IDENTIFIER:
				bgplsID = ((BGPLSIdentifierNodeDescriptorSubTLV) nodeDescriptorsSubTLV.get(i)).getBGPLS_ID();
				//log.info("BGPLS IDENTIFIER found in LINK_NLRI(local_node). bgpls_id "+bgplsID);
				continue;

			case NodeDescriptorsSubTLVTypes.NODE_DESCRIPTORS_SUBTLV_TYPE_IGP_ROUTER_ID:
				LocalNodeIGPId = ((IGPRouterIDNodeDescriptorSubTLV)nodeDescriptorsSubTLV.get(i)).getIpv4AddressOSPF();
				//log.info("IGP ROUTER ID found in LINK_NLRI(local_node). igp_id "+LocalNodeIGPId);
				continue;		
			default:
				log.finest("Attribute Code unknown");
			}
		}
		//no haria falta este for pero bueno, lo dejamos como recuerdo de @mcs
		nodeDescriptorsSubTLV = linkNLRI.getRemoteNodeDescriptorsTLV().getNodeDescriptorsSubTLVList();
		for (int i = 0;i<nodeDescriptorsSubTLV.size();i++){
			int subTLVType = nodeDescriptorsSubTLV.get(i).getSubTLVType();
			switch (subTLVType){	
			case NodeDescriptorsSubTLVTypes.NODE_DESCRIPTORS_SUBTLV_TYPE_AUTONOMOUS_SYSTEM:
				remoteDomainID = ((AutonomousSystemNodeDescriptorSubTLV) nodeDescriptorsSubTLV.get(i)).getAS_ID();
				//log.info("AUTONOMOUS_SYSTEM found in LINK_NLRI(remote_node). as_remote "+remoteDomainID);
				continue;
			case NodeDescriptorsSubTLVTypes.NODE_DESCRIPTORS_SUBTLV_TYPE_AREA_ID:
				areaID = ((AreaIDNodeDescriptorSubTLV) nodeDescriptorsSubTLV.get(i)).getAREA_ID();
				//log.info("AREA_ID found in LINK_NLRI(remote_node). area_id "+areaID);
				continue;

			case NodeDescriptorsSubTLVTypes.NODE_DESCRIPTORS_SUBTLV_TYPE_BGP_LS_IDENTIFIER:
				bgplsID = ((BGPLSIdentifierNodeDescriptorSubTLV) nodeDescriptorsSubTLV.get(i)).getBGPLS_ID();
				//log.info("BGPLS IDENTIFIER found in LINK_NLRI(remote_node). bgpls_id "+bgplsID);
				continue;

			case NodeDescriptorsSubTLVTypes.NODE_DESCRIPTORS_SUBTLV_TYPE_IGP_ROUTER_ID:
				RemoteNodeIGPId = ((IGPRouterIDNodeDescriptorSubTLV)nodeDescriptorsSubTLV.get(i)).getIpv4AddressOSPF();
				//log.info("IGP ROUTER ID found in LINK_NLRI(remote_node). igp_id "+RemoteNodeIGPId);
				continue;

			default:
				log.finest("Attribute Code unknown");
			}
		}

		/**Creamos el grafo*/
		//Let's see if our link is intradomain or interdomain...
		//log.info("as_local "+localDomainID);
		//log.info("as_remote "+remoteDomainID);

		if(localDomainID.equals(remoteDomainID)){
			//log.info("INTRADOMAIN...");
			IntraDomainEdge intraEdge = new IntraDomainEdge();
			if (linkNLRI.getLinkIdentifiersTLV() != null){				
				intraEdge.setSrc_if_id(linkNLRI.getLinkIdentifiersTLV().getLinkLocalIdentifier());
				intraEdge.setDst_if_id(linkNLRI.getLinkIdentifiersTLV().getLinkRemoteIdentifier());						
			}					

			if (simpleTEDB.getNetworkGraph() == null){					
				simpleTEDB.createGraph();
			}

			/**Actualizando TED*/
			//log.info("lET'S SEE WHAT DO WE HAVE TO UPDATE...");


			//log.info("Found Vertex:"+LocalNodeIGPId.toString());
			if (!(simpleTEDB.getNetworkGraph().containsVertex(LocalNodeIGPId))){
				log.info("Adding Local Vertex... " + LocalNodeIGPId.toString());
				simpleTEDB.getNetworkGraph().addVertex(LocalNodeIGPId);//add vertex ya comprueba si existe el nodo en la ted-->se puede hacer mas limpio
			}
			else{ 
				log.info("Local Vertex: "+LocalNodeIGPId.toString() +" already present in TED...");
			}
			//log.info("Found Vertex: "+RemoteNodeIGPId.toString());
			if (!(simpleTEDB.getNetworkGraph().containsVertex(RemoteNodeIGPId))){
				log.info("Adding Remote Vertex... " + RemoteNodeIGPId.toString());
				simpleTEDB.getNetworkGraph().addVertex(RemoteNodeIGPId);

			}
			else {
				log.info("Remote Vertex: "+RemoteNodeIGPId.toString() +" already present in TED...");
			}

			te_info =  createTE_Info();
			intraEdge.setTE_info(te_info);
			intraEdge.setLearntFrom(learntFrom);

			if (!(simpleTEDB.getNetworkGraph().containsEdge(LocalNodeIGPId, RemoteNodeIGPId))){
				log.info("Adding information of local node to edge..." + simpleTEDB.getNodeTable().get(LocalNodeIGPId));
				intraEdge.setLocal_Node_Info(simpleTEDB.getNodeTable().get(LocalNodeIGPId));
				log.info("Adding information of remote node to edge..." + simpleTEDB.getNodeTable().get(RemoteNodeIGPId));
				intraEdge.setRemote_Node_Info(simpleTEDB.getNodeTable().get(RemoteNodeIGPId));
				log.info("Adding edge from origin vertex"+LocalNodeIGPId.toString()+ " to destination vertex" +RemoteNodeIGPId.toString());
				simpleTEDB.getNetworkGraph().addEdge(LocalNodeIGPId, RemoteNodeIGPId, intraEdge);
				IntraDomainEdge edge=simpleTEDB.getNetworkGraph().getEdge(LocalNodeIGPId, RemoteNodeIGPId);
				if(intraEdge.getTE_info().getAvailableLabels()!=null)
				((BitmapLabelSet)edge.getTE_info().getAvailableLabels().getLabelSet()).initializeReservation(((BitmapLabelSet)intraEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap());

			}
			else{
				log.info("Link already present in TED, let's update the available labels...");
				IntraDomainEdge edge;
				edge=simpleTEDB.getNetworkGraph().getEdge(LocalNodeIGPId, RemoteNodeIGPId);
				if(((BitmapLabelSet)this.availableLabels.getLabelSet()).getDwdmWavelengthLabel()!=null){
					((BitmapLabelSet)edge.getTE_info().getAvailableLabels().getLabelSet()).arraycopyBytesBitMap(((BitmapLabelSet)intraEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap());
					log.info("Reservation1: " + ((BitmapLabelSet)edge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[0]);
					if (((BitmapLabelSet)intraEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved()!=null){
						((BitmapLabelSet)edge.getTE_info().getAvailableLabels().getLabelSet()).arraycopyReservedBytesBitMap(((BitmapLabelSet)intraEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved());
						log.info("Reservation2: " + ((BitmapLabelSet)edge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved()[3]);
					}
				}
			}
		}

		else{
			log.info("INTERDOMAIN...");
			InterDomainEdge interEdge = new InterDomainEdge();
			if (linkNLRI.getLinkIdentifiersTLV() != null){				
				interEdge.setSrc_if_id(linkNLRI.getLinkIdentifiersTLV().getLinkLocalIdentifier());
				interEdge.setDst_if_id(linkNLRI.getLinkIdentifiersTLV().getLinkRemoteIdentifier());						
			}
			//terminamos de caracterizar el interedge...
			interEdge.setSrc_router_id(LocalNodeIGPId);
			interEdge.setDst_router_id(RemoteNodeIGPId);
			interEdge.setDomain_dst_router(remoteDomainID);

			/**Actualizando TED*/
			//log.info("Updating Interdomain list...");
			te_info =  createTE_Info();
			interEdge.setTE_info(te_info);
			interEdge.setLearntFrom(learntFrom);

			/**
			if(simpleTEDB.getInterdomainLink(LocalNodeIGPId, RemoteNodeIGPId)!= null){
				log.info("entro al update interdomain!!!");
				log.info("Origen: " + interEdge.getSrc_router_id() + "Destino: " + interEdge.getDst_router_id());
				log.info("Edge: " + interEdge.toString());
				InterDomainEdge edge = simpleTEDB.getInterdomainLink(LocalNodeIGPId, RemoteNodeIGPId);
				//edge.setTE_info(te_info);
				//int index = simpleTEDB.getInterDomainLinks().indexOf(interEdge);
				if(((BitmapLabelSet)this.availableLabels.getLabelSet()).getDwdmWavelengthLabel()!=null){
					((BitmapLabelSet)edge.getTE_info().getAvailableLabels().getLabelSet()).arraycopyBytesBitMap(((BitmapLabelSet)interEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap());
					if (((BitmapLabelSet)interEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved()!=null)
						((BitmapLabelSet)edge.getTE_info().getAvailableLabels().getLabelSet()).arraycopyReservedBytesBitMap(((BitmapLabelSet)interEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitmapReserved());
				}
			}*/	
			
			if(simpleTEDB.getInterdomainLink(LocalNodeIGPId, RemoteNodeIGPId) == null){
				simpleTEDB.getInterDomainLinks().add(interEdge);
				InterDomainEdge edge = simpleTEDB.getInterdomainLink(LocalNodeIGPId, RemoteNodeIGPId);
				((BitmapLabelSet)edge.getTE_info().getAvailableLabels().getLabelSet()).initializeReservation(((BitmapLabelSet)interEdge.getTE_info().getAvailableLabels().getLabelSet()).getBytesBitMap());
			}
			
			//simpleTEDB.getInterDomainLinks().add(interEdge);
			log.info("Adding interdomain link tu multited...");
			multiTedb.addInterdomainLink(localDomainID, LocalNodeIGPId, linkNLRI.getLinkIdentifiersTLV().getLinkLocalIdentifier(), remoteDomainID, RemoteNodeIGPId, linkNLRI.getLinkIdentifiersTLV().getLinkRemoteIdentifier(), te_info);


		}
	} 

	private TE_Information createTE_Info(){
		TE_Information te_info = new TE_Information();
		if (maximumLinkBandwidthTLV.getTLVValueLength() != 0){
			MaximumBandwidth maximumBandwidth = new MaximumBandwidth();
			maximumBandwidth.setMaximumBandwidth(maximumLinkBandwidthTLV.getMaximumBandwidth());
			te_info.setMaximumBandwidth(maximumBandwidth);
		}
		if (maxReservableBandwidthTLV.getTLVValueLength() != 0){
			MaximumReservableBandwidth maximumReservableBandwidth = new MaximumReservableBandwidth();
			maximumReservableBandwidth.setMaximumReservableBandwidth(maxReservableBandwidthTLV.getMaximumReservableBandwidth());
			te_info.setMaximumReservableBandwidth(maximumReservableBandwidth);
		}
		if (unreservedBandwidthTLV.getTLVValueLength() != 0){
			UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth();
			unreservedBandwidth.setUnreservedBandwidth(unreservedBandwidthTLV.getUnreservedBandwidth());
			te_info.setUnreservedBandwidth(unreservedBandwidth);
		}
		if(iPv4RouterIDLocalNodeLATLV.getTLVValueLength() != 0){
			IPv4RouterIDLocalNodeLinkAttribTLV iPv4RouterIDLocalNode = new IPv4RouterIDLocalNodeLinkAttribTLV();
			iPv4RouterIDLocalNode.setIpv4Address(iPv4RouterIDLocalNodeLATLV.getIpv4Address());
			te_info.setiPv4LocalNode(iPv4RouterIDLocalNode);
		}
		if(iPv4RouterIDRemoteNodeLATLV.getTLVValueLength()!=0){
			IPv4RouterIDRemoteNodeLinkAttribTLV iPv4RouterIDRemoteNode = new IPv4RouterIDRemoteNodeLinkAttribTLV();
			iPv4RouterIDRemoteNode.setIpv4Address(iPv4RouterIDRemoteNodeLATLV.getIpv4Address());
			te_info.setiPv4RemoteNode(iPv4RouterIDRemoteNode);
		}
		if(metricTLV.getTLVValueLength()!=0){
			MetricLinkAttribTLV metric = new MetricLinkAttribTLV();
			metric.setMetric(metricTLV.getMetric());
			te_info.setMetric(metric);
		}
		if(TEMetricTLV.getTLVValueLength()!=0){
			TrafficEngineeringMetric teMetric = new TrafficEngineeringMetric();
			teMetric.setLinkMetric((long)TEMetricTLV.getLinkMetric());
			te_info.setTrafficEngineeringMetric(teMetric);
		}
		if(administrativeGroupTLV.getTLVValueLength()!=0){
			AdministrativeGroup adminGroup = new AdministrativeGroup();
			adminGroup.setAdministrativeGroup(administrativeGroupTLV.getAdministrativeGroup());
			te_info.setAdministrativeGroup(adminGroup);
		}
		if(linkProtectionTLV.getTLVValueLength()!=0){
			LinkProtectionTypeLinkAttribTLV linkProtection = new LinkProtectionTypeLinkAttribTLV();
			linkProtection.setProtection_type(linkProtectionTLV.getProtection_type());
			te_info.setLinkProtectionBGPLS(linkProtection);
		}
		if (availableLabels.getTLVValueLength()!= 0){
			if(((BitmapLabelSet)this.availableLabels.getLabelSet()).getDwdmWavelengthLabel()!=null){
				if(simpleTEDB.getSSONinfo()==null){
					log.info("NEW SSON INFO");
					SSONInformation ssonInfo = new SSONInformation();
					ssonInfo.setCs(((BitmapLabelSet)this.availableLabels.getLabelSet()).getDwdmWavelengthLabel().getChannelSpacing());
					ssonInfo.setGrid(((BitmapLabelSet)this.availableLabels.getLabelSet()).getDwdmWavelengthLabel().getGrid());
					ssonInfo.setNumLambdas(((BitmapLabelSet)this.availableLabels.getLabelSet()).getNumLabels());
					ssonInfo.setCommonAvailableLabels(this.availableLabels);
					ssonInfo.setnMin(0);
					simpleTEDB.setSSONinfo(ssonInfo);
				}
				if(simpleTEDB.getWSONinfo()==null){
					log.info("NEW WSON INFO");
					WSONInformation wsonInfo = new WSONInformation();
					wsonInfo.setCs(((BitmapLabelSet)this.availableLabels.getLabelSet()).getDwdmWavelengthLabel().getChannelSpacing());
					wsonInfo.setGrid(((BitmapLabelSet)this.availableLabels.getLabelSet()).getDwdmWavelengthLabel().getGrid());
					wsonInfo.setNumLambdas(((BitmapLabelSet)this.availableLabels.getLabelSet()).getNumLabels());
					wsonInfo.setCommonAvailableLabels(this.availableLabels);
					wsonInfo.setnMin(0);
					simpleTEDB.setWSONinfo(wsonInfo);
				}
			}
			te_info.setAvailableLabels(availableLabels);
		}
		return te_info;
	}
	private void fillNodeInformation(NodeNLRI nodeNLRI, String learntFrom){
		log.info("Let's fill in the node information.....");

		ArrayList<NodeDescriptorsSubTLV> nodeDescriptorsSubTLV;
		nodeDescriptorsSubTLV =  nodeNLRI.getLocalNodeDescriptors().getNodeDescriptorsSubTLVList();
		Inet4Address as_number = null;
		Inet4Address areaID= null ;
		Inet4Address bgplsID = null;
		int IGP_type = 0;
		Inet4Address IGPID = null;
		Node_Info node_info = new Node_Info();
		Hashtable<Inet4Address , Node_Info> NodeTable;
		//ArrayList<Inet4Address> address = new ArrayList<Inet4Address>();
		for (int i = 0;i<nodeDescriptorsSubTLV.size();i++){
			int subTLVType = nodeDescriptorsSubTLV.get(i).getSubTLVType();
			switch (subTLVType){	
			case NodeDescriptorsSubTLVTypes.NODE_DESCRIPTORS_SUBTLV_TYPE_AUTONOMOUS_SYSTEM:
				as_number = ((AutonomousSystemNodeDescriptorSubTLV) nodeDescriptorsSubTLV.get(i)).getAS_ID();
				log.info("adding AS number of local node to table......"+as_number.toString());
				node_info.setAs_number(as_number);
				continue;
			case NodeDescriptorsSubTLVTypes.NODE_DESCRIPTORS_SUBTLV_TYPE_AREA_ID:
				areaID = ((AreaIDNodeDescriptorSubTLV) nodeDescriptorsSubTLV.get(i)).getAREA_ID();
				log.info("adding AreaID of local node to table......"+areaID.toString());
				node_info.setArea_id(areaID);
				continue;
			case NodeDescriptorsSubTLVTypes.NODE_DESCRIPTORS_SUBTLV_TYPE_BGP_LS_IDENTIFIER:
				bgplsID = ((BGPLSIdentifierNodeDescriptorSubTLV) nodeDescriptorsSubTLV.get(i)).getBGPLS_ID();
				log.info("adding BGPLS identifier of local node to table......"+bgplsID.toString());
				node_info.setBgpls_ident(bgplsID);
				continue;
			case NodeDescriptorsSubTLVTypes.NODE_DESCRIPTORS_SUBTLV_TYPE_IGP_ROUTER_ID:
				IGP_type = ((IGPRouterIDNodeDescriptorSubTLV) nodeDescriptorsSubTLV.get(i)).getIGP_router_id_type();
				switch(IGP_type){
				case 3:
					IGPID = ((IGPRouterIDNodeDescriptorSubTLV) nodeDescriptorsSubTLV.get(i)).getIpv4AddressOSPF();
					log.info("adding IGP ID of local node to table......"+IGPID.toString());
					node_info.setIpv4Address(IGPID);
					continue;
				default:
					log.info("añadir este tipo de IGP Identifier por implementar ");
				}
				continue;
			default:
				log.finest("Attribute Code unknown");
			}
		}

		log.info("Let's fill in the node information table with the node's attributes...");
		if(iPv4RouterIDLocalNodeLATLV.getTLVValueLength() != 0){
			log.info("adding ipv4 of local node to table......");
			node_info.setIpv4AddressLocalNode(iPv4RouterIDLocalNodeLATLV.getIpv4Address());
		}
		if(nodeFlagBitsTLV.getTLVValueLength() != 0){
			log.info("adding flags of local node to table...");
			node_info.setAbr_bit(nodeFlagBitsTLV.isAbr_bit());
			node_info.setAttached_bit(nodeFlagBitsTLV.isAttached_bit());
			node_info.setExternal_bit(nodeFlagBitsTLV.isExternal_bit());
			node_info.setOverload_bit(nodeFlagBitsTLV.isOverload_bit());
		}

		if(nodeNameTLV.getTLVValueLength() != 0){
			log.info("adding name of local node to table....");
			node_info.setName(nodeNameTLV.getName());
		}

		if(areaIDTLV.getTLVValueLength() != 0){
			log.info("adding areaID of local node to table....");
			node_info.setIpv4areaIDs(areaIDTLV.getIpv4areaIDs());
		}

		if(sidTLV.getTLVValueLength() != 0){
			log.info("adding SID of local node to table....");
			node_info.setSID(sidTLV.getSid());
		}
		//.... finally we set the 'learnt from' attribute
		node_info.setLearntFrom(learntFrom);
		log.info("learnt from: " +learntFrom);
		NodeTable = simpleTEDB.getNodeTable();

		//if the table already contains the node it replaces it
		if(NodeTable!=null){
			if(NodeTable.containsKey(IGPID))
				NodeTable.remove(IGPID);
		}

		if(node_info != null)
			NodeTable.put(IGPID, node_info);

		simpleTEDB.setNodeTable(NodeTable);
		if (this.multiTedb!=null) {
			if (node_info.getIpv4Address()!=null){
				this.multiTedb.addReachabilityIPv4(as_number, node_info.getIpv4Address(), 32);
			}
			
		}
		log.info("Node Table:" + NodeTable.toString());
		log.info("Node Information Table Updated....");

	}



}
