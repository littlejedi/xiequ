package org.jedi.wow;

import java.util.List;
import com.google.common.base.Objects;

public class RaidInfo {
	
	private String raidLeader = "P总";
	
	private String description;
	
	private String time;
	
	private List<RaidMember> members;

	public String getRaidLeader() {
		return raidLeader;
	}

	public void setRaidLeader(String raidLeader) {
		this.raidLeader = raidLeader;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public List<RaidMember> getMembers() {
		return members;
	}

	public void setMembers(List<RaidMember> members) {
		this.members = members;
	}

	@Override
	public int hashCode(){
		return Objects.hashCode(raidLeader, description, time, members);
	}
	
	@Override
	public boolean equals(Object object){
		if (object instanceof RaidInfo) {
			RaidInfo that = (RaidInfo) object;
			return Objects.equal(this.raidLeader, that.raidLeader)
				&& Objects.equal(this.description, that.description)
				&& Objects.equal(this.time, that.time)
				&& Objects.equal(this.members, that.members);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("raidLeader", raidLeader)
			.add("description", description)
			.add("time", time)
			.add("members", members)
			.toString();
	}
}
