
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * <p>Title: ChatUserMgr</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: WebEx Inc.</p>
 * @author not attributable
 * @version 2004.03.06 fix bug#97992 and expel Presenter
 * @version 2004.04.15 for CC1.4 features
 * @version 2004.05.15 for fix bug #100748
 * @version 2004.05.27 for fix bug #100032, #100440
 * @version 2004.06.10 for avoid enroll again before enroll confirm back.
 * @version 2004.11.18 EC T21
 * @version 2006.01.09 modified for fix bug#189334. Gets the default send-to channel ID.
 * @version 2006.12.20 modified for following native TypingIndincation implementation.
 * @version 2007.01.05 modified for rollback the TypingIndincation implementation.
 * @version 2007.03.15 modified for Flash-Client.
 * @version 2007.03.21 modified for SC user-binding with guest-id (NOTE: It does not 
 *  involve buffered Host, Presenter references consideration because it is SC only).
 * @version 2007.03.29 modified for considering negative guest-id.
 * @version 2008.04.03 modified for fixing bug#284685.
 * @version 2008.04.12 Modified for fix bug#285766.
 * @version 2008.05.02 modified for ChatPDU change and code refactory.
 */
public class ChatUserMgr
{
	private static int UNKNOW_UID = -1000;
	// User list:
	private List m_chatUsers;
	// My self info (ChatUser). Buffered its reference for performance.
	public ChatUser m_me = null;

	// Chat utilities.
	private ChatUtil m_util;

	// The ChatUserInfo object list for recovery.
	private List m_userInfos;

	// The most recent chat-user list:
	private java.util.List m_mruList = null;

	// the FAQ default target node-id:
	private int _faq_default_nodeid;

	/**
	 * Chat use rmanager constructor.
	 * @param util ChatUtil
	 */
	public ChatUserMgr(ChatUtil util)
	{
		m_chatUsers = new ArrayList();
		m_mruList = new ArrayList();
		m_util = util;
		m_me = new ChatUser(m_util, UNKNOW_UID--, "Myself", 0, 0);
		m_chatUsers.add(m_me);
		m_userInfos = new ArrayList();
	}

	public Vector getUserListBy(int roleSet)
	{
		Vector ulist=new Vector();
		for (int i=0; i<m_chatUsers.size(); i++)
		{
			ChatUser user = (ChatUser)m_chatUsers.get(i);
			if( user.isRole(roleSet) && ! isMe( user ) )
				ulist.add(user);
		}
		return ulist;
	}

	/**
	 * Is support typing-indication feature?
	 * @return
	 */
	public synchronized boolean isSupportTypingIndication()
	{
		return m_chatUsers.size() > 1 
			&& m_chatUsers.size() <= m_util.getTypingIndMax();
	}

	/**
	 * Get FAQ default target user.
	 * @return ChatUser The FAQ default-target-user.
	 */
	public synchronized ChatUser getFaqDefaultUser()
	{
		for(int i=0; i<m_chatUsers.size(); i++)
		{
			ChatUser user = (ChatUser)m_chatUsers.get(i);
			if(user.node_id == _faq_default_nodeid)
				return user;
		}
		return null;
	}

	/**
	 * Sets the primary-customer status in SC.
	 * @param pri_cust_id The FAQ default target node-id.
	 */
	public synchronized void setFaqDefaultNodeId(int pri_cust_id)
	{
		DumpMsg(1, "FAQ default node-id: "+pri_cust_id);
		_faq_default_nodeid = pri_cust_id;
	}

	public synchronized ChatUser getUserByRole(int role){
		for(int i=0; i<m_chatUsers.size(); i++){
			ChatUser user = (ChatUser)m_chatUsers.get(i);
			if(user.isRole(role)) return user;
		}
		return null;
	}

	/**
	 * Gets host user instance. For TC, EC and MC only
	 * @return ChatUser
	 */
	public ChatUser getHost()
	{
		return getUserByRole(IAtlChat.HOST_ROLE);
	}

	/**
	 * Gets presenter user instance. For TC, EC and MC only
	 * @return ChatUser
	 */
	public ChatUser getPresenter()
	{
		return getUserByRole(IAtlChat.PRESENTER_ROLE);
	}

	/**
	 * Add a unknow to user list.
	 * @param node_id int
	 * @param role_set int
	 * @return ChatUser
	 */
	public synchronized ChatUser addUnknownUser(int node_id, int role_set)
	{
		DumpMsg(0, "###### addUnknownUser: node_id =" +node_id+"role_set = "+role_set);
		ChatUser user = new ChatUser(m_util, UNKNOW_UID--, "Unknown"+UNKNOW_UID, node_id, role_set);
		m_chatUsers.add(user);
		return user;
	}

	public synchronized ChatUser getUserByNodeID(int nodeID)
	{
		if(nodeID == 0) {
			DumpMsg(0, "###### getUserByNodeID() return null because nodeId==0!");
			return null;
		}
		for(int i = 0; i < m_chatUsers.size(); i++)
		{
			ChatUser u = (ChatUser)m_chatUsers.get(i);
			if(u.node_id == nodeID)
				return u;
			u = u.getUserByNodeID(nodeID);
			if(u != null) {
				DumpMsg(0, "### getUserByNodeID() return a bound-in user, nodeId="+nodeID);
				return u;
			}
		}
		DumpMsg(1, "getUserByNodeID() return null, nodeId="+nodeID);
		return null;
	}

	public synchronized ChatUser getUserByUserID(int userID)
	{
		if(userID == 0) {
			DumpMsg(0, "###### getUserByUserID() return null because userId==0!");
			return null;
		}
		for(int i = 0; i < m_chatUsers.size(); i++)
		{
			ChatUser u = (ChatUser)m_chatUsers.get(i);
			if(u.user_id == userID)
				return u;
			u = u.getUserByUserID(userID);
			if(u != null) {
				DumpMsg(0, "### getUserByUserID() return a bound-in user, userId="+userID);
				return u;
			}
		}
		DumpMsg(1, "getUserByUserID() return null, userId="+userID);
		return null;
	}

	public synchronized ChatUser getUserByGuestID(int guest_id)
	{
		if(guest_id == 0)
			return null;
		for(int i = 0; i < m_chatUsers.size(); i++)
		{
			ChatUser u = (ChatUser)m_chatUsers.get(i);
			if(u.getGuestID() == guest_id)
			{
				return u;
			}
		}
		return null;
	}

	/**
	 * On roster changed.
	 * @param user
	 */
  	public synchronized void onUserChanged(ChatUser user)
	{
		if(user == null) return;
		ChatUser u = getUserByGuestID(user.getGuestID());
		if(!m_chatUsers.contains(user))
		{
			DumpMsg(0, "add user: "+user);
			m_chatUsers.add(user);
			if(u != null && u != user)
			{
				m_chatUsers.remove(u);
				if(m_mruList.contains(u))
				{
					m_mruList.remove(u);
					m_mruList.add(user);
				}
				u.unbindUser(user);
				user.bindUser(u);
			}
		}
		java.util.Collections.sort(m_chatUsers);
	}

	/**
	 * Removes the user.
	 * @param user ChatUser
	 */
	public synchronized void removeTheUser(ChatUser user)
	{
		if(user==null) return;
		DumpMsg(0, "removeTheUser: "+user);
		ChatUser u = user.getBoundUser(0);
		if(m_chatUsers.contains(user))
		{
			if(u != null && u != user)
			{
				user.unbindUser(u);
				m_chatUsers.add(u);
				if(m_mruList.contains(user))
				{
					m_mruList.add(u);
				}
			}
			m_mruList.remove(user);
			m_chatUsers.remove(user);
		}
		else if(u != null && u != user)
		{
			u.unbindUser(user);
		}
	}

	public synchronized void removeAllUsers()
	{
		this.m_chatUsers.clear();
	}

	/**
	 * He sent a chat message to me.
	 * @param user ChatUser
	 * @return boolean
	 */
	public boolean addUserToMRU(ChatUser user)
	{
		if(this.isMe(user) || !m_chatUsers.contains(user))
			return false;
		
		user.recordLastChatTime();
		int ui = m_mruList.indexOf(user);
		if(ui<0) {
			m_mruList.add(0, user);
			return true;
		}
		else if(ui>0)
		{
			m_mruList.remove(user);
			m_mruList.add(0, user);
			return true;
		}
		return false;
	}

	public boolean isMe(ChatUser user)
	{
		return m_me.isSameUser(user);
	}

	/**
	 * Get user role.
	 * @param nodeId int
	 * @return int
	 * @version 2004.11.18 for EC T21.
	 */
	protected synchronized int getUserRoleSet(int nodeId)
	{
		ChatUser user = getUserByNodeID(nodeId);
		if(user == null)
			return 0;
		return user.roleSet();
	}

	/**
	 * Adds users in a group to SendTo.
	 * @param chatChoice List
	 * @param group ChatGroup
	 * @param splitLine SendToItem
	 * @return SendToItem The last split line need to be added.
	 */
	public synchronized ChatSendToItem addSendToForGroup(List chatChoice, ChatGroup group, ChatSendToItem splitLine)
	{
		//DumpMsg(1, "Add users to group "+group.getTitle()+", role-set="+group.allRoleSet());
		int myUserId = m_me.user_id;
		List members = new ArrayList();
		if (group.getMruBeginTime() != 0)
		{
			// get MRU members SendTo list:
			for (int i = 0; i < m_mruList.size(); i++)
			{
				ChatUser u = (ChatUser) m_mruList.get(i);
				if (u.lastChatTime() < group.getMruBeginTime())
				{
					continue;
				}
				// Try to add the user into member list;
				if(group.isMember(u) && u.user_id != myUserId && u.isVisible())
				{
					ChatSendToItem item = u.getSendToItem();
					if (chatChoice.contains(item) || members.contains(item))
					{
						item = new ChatSendToItem(u, null);
					}
					members.add(item);
					if(members.size() >= group.getMruMaxCount())
						break;
				}
			}
		}
		else
		{
			// get All members SendTo list:
			for(int i=0; i<m_chatUsers.size(); i++)
			{
				ChatUser u = (ChatUser)m_chatUsers.get(i);
				if(u == null)
					continue ;
				// Try to add the user into member list;
				if(group.isMember(u) && u.user_id != myUserId && u.isVisible())
				{
					ChatSendToItem item = u.getSendToItem();
					if(chatChoice.contains(item) || members.contains(item))
					{
						item = new ChatSendToItem(u, null);
					}
					members.add(item);
				}
			}
		}
		if(members.size()>0)
		{
			if(splitLine!=null && chatChoice.size()>0)
			{
				chatChoice.add(splitLine);
				splitLine = null;
			}
			chatChoice.addAll(members);
		}
		return splitLine;
	}

	/**
	 * Gets group user-list.
	 * @param group
	 * @return The group user-list.
	 */
//	public synchronized List getGroupUserList(ChatGroup group)
//	{
//		List guList = new ArrayList();
//		for(int i = 0; i < m_chatUsers.size(); i++)
//		{
//			ChatUser u = (ChatUser)m_chatUsers.get(i);
//			if(group.isMember(u))
//				guList.add(u);
//		}
//		return guList;
//	}

	/**
	 * Has any panelists? For MC, TC, EC only.
	 * @return boolean
	 * @version 2004.11.18 for EC T21.
	 */
	public synchronized boolean hasPanelistsHP()
	{
		for(int i = 0; i < m_chatUsers.size(); i++)
		{
			ChatUser u = (ChatUser)m_chatUsers.get(i);
			if(u == null)
				continue;
			if(u.isHost() || u.isPresenter() || u.isPanelist())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * On chat session ready.
	 * @param nodeId int
	 * @param userName String
	 * @version 2004.11.18 for EC T21.
	 */
	public synchronized void onChatSessionReady(int nodeId, String userName)
	{
		m_me.setName(userName);
		m_me.node_id = nodeId;
		m_chatUsers.remove(m_me);
		ChatUser oldMe = getUserByNodeID(nodeId);
		if(oldMe!=null /*&& !m_me.equals(oldMe)*/)
		{
			this.m_chatUsers.remove(oldMe);
			m_me.setHost(oldMe.isHost());
			m_me.setPresenter(oldMe.isPresenter());
			m_me.setPanelist(oldMe.isPanelist());
		}
		this.m_chatUsers.add(m_me);
	}

	/**
	 * I am Host or Presenter or Panelist? For TC, EC only
	 * @return boolean
	 * @version 2004.11.17 for EC T21.
	 */
	public synchronized boolean ImHPP()
	{
		return (m_me.isHost() || m_me.isPresenter() || m_me.isPanelist());
	}

	/**
	 * I am Host or Presenter? For TC, EC and MC.
	 * @return boolean
	 * @version 2004.11.17 for EC T21.
	 */
	public synchronized boolean ImHP()
	{
		return (m_me.isHost() || m_me.isPresenter());
	}

	/////////   ChatUserInfo list manager for recovery    /////////

	public synchronized void addChatUserInfo(ChatUserInfo uinfo)
	{
		DumpMsg(1, "added a user-info uid = "+uinfo._apeUserID+", name = "+uinfo._userName);
		m_userInfos.add(uinfo);
	}

	public synchronized ChatUserInfo removeChatUserInfo(int uid)
	{
		for(int i=0; i<m_userInfos.size(); i++)
		{
			ChatUserInfo uinfo = (ChatUserInfo)m_userInfos.get(i);
			if(uinfo._apeUserID==uid)
			{
				DumpMsg(1, "remove a user-info uid = "+uinfo._apeUserID+", name = "+uinfo._userName);
				m_userInfos.remove(uinfo);
				return uinfo;
			}
		}
		return null;
	}

	public synchronized ChatUser getUserFromInfoListByUID(int uid)
	{
		for(int i=0; i<m_userInfos.size(); i++)
		{
			ChatUserInfo uinfo = (ChatUserInfo)m_userInfos.get(i);
			if(uinfo._apeUserID==uid)
			{
				ChatUser user = new ChatUser(m_util, uid, uinfo._userName, 0, m_util.base_role());
				return user;
			}
		}
		return null;
	}

	////// END of ChatUserInfo list manager for recovery  /////////

	/**
	 * Dump debug message.
	 * @param level int
	 * @param str String
	 */
	private void DumpMsg(int level, String str)
	{
		if(level<=ChatUtil.LOG_LEVEL)
			ChatUtil.DumpMsg("[ChatUserMgr] "+str);
	}
}
