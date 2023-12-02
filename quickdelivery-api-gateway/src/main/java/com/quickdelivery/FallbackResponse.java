package com.quickdelivery;

/*import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString*/
public class FallbackResponse {

	public Integer getMsgCode() {
		return msgCode;
	}

	public void setMsgCode(Integer msgCode) {
		this.msgCode = msgCode;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	private Integer msgCode;
	private String msg;



}
