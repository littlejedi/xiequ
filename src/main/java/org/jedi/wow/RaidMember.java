package org.jedi.wow;

import com.google.common.base.Objects;

public class RaidMember {
	
	private String openid;
	
	private String nickname;
	
	private boolean isGoingToRaid;
	
	public String getOpenid() {
		return openid;
	}

	public void setOpenid(String openid) {
		this.openid = openid;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public boolean isGoingToRaid() {
		return isGoingToRaid;
	}

	public void setGoingToRaid(boolean isGoingToRaid) {
		this.isGoingToRaid = isGoingToRaid;
	}

	@Override
	public int hashCode(){
		return Objects.hashCode(openid, nickname, isGoingToRaid);
	}
	
	@Override
	public boolean equals(Object object){
		if (object instanceof RaidMember) {
			RaidMember that = (RaidMember) object;
			return Objects.equal(this.openid, that.openid)
				&& Objects.equal(this.nickname, that.nickname)
				&& Objects.equal(this.isGoingToRaid, that.isGoingToRaid);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("openid", openid)
			.add("nickname", nickname)
			.add("isGoingToRaid", isGoingToRaid)
			.toString();
	}
}
