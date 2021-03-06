package com.rs.wxmgr.action;

import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.rs.wxmgr.action.entity.TXResponse;
import com.rs.wxmgr.action.entity.TXResponseFactory;
import com.rs.wxmgr.common.content.TXContentFlag;
import com.rs.wxmgr.common.content.TXErrorCode;
import com.rs.wxmgr.common.exception.TXException;
import com.rs.wxmgr.entity.WelcomeMsg;
import com.rs.wxmgr.service.TestService;
import com.rs.wxmgr.service.WXRobotFactory;
import com.rs.wxmgr.wechat.Robot;

@Controller
public class RobotAction {
	
	private static final Logger logger = LoggerFactory.getLogger(RobotAction.class);
	
	@Autowired
	private TestService testService;
	@Autowired
	private WXRobotFactory robotFactory;
	
	private Robot robot;

	@ResponseBody
	@RequestMapping("/getWelcomeMsgList")
	public JSONObject getWelcomeMsgList(HttpServletRequest request) {
		TXResponse response = TXResponseFactory.CreateSuccess();
		try {
			List<WelcomeMsg> messageList = testService.selectMessageList();
			response.put("rows", messageList);
		} catch (TXException e) {
			logger.error(e.getMessage(), e);
			response = TXResponseFactory.CreateFail(TXContentFlag.TX_EXCEPTION_CODE,e.getMessage());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			response = TXResponseFactory.CreateFail(TXErrorCode.SYSTEMRROR);
		}
		return response.getData();
	}
	
	@ResponseBody
	@RequestMapping(value="/addWelcomeMsg")
	public JSONObject addWelcomeMsg(@RequestBody HashMap<String,Object> data){
		TXResponse response = TXResponseFactory.CreateSuccess();
		try {
			String message = data.get("message").toString();
			if(StringUtils.isNotBlank(message)) {
				testService.insertMessage(message);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			response = TXResponseFactory.CreateFail(TXErrorCode.SYSTEMRROR);
		}
		return response.getData();
	}
	
	/**
	 * 登录二维码
	 * @param request
	 * @param response
	 */
	@RequestMapping(value="/wechatlogin")
	public void wechatlogin(HttpServletRequest request, HttpServletResponse response){
		try {
			if(robot==null || robot.isClose()) {
				robot = robotFactory.createRobot();
			}
			response.getOutputStream().write(robot.getRQCode());
			robot.checkLogin();
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}
	
	/**
	 * 检查微信机器人是否在线
	 * @param request
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/checklogin")
	public JSONObject checklogin(HttpServletRequest request){
		TXResponse response = TXResponseFactory.CreateSuccess();
		try {
			if(robot == null) {
				response.put("data", false);
			} else {
				response.put("data", robot.isOnline());
			}
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return response.getData();
	}
	
	/**
	 * 测试获取好友列表
	 * @param request
	 */
	@ResponseBody
	@RequestMapping(value="/getmember")
	public JSONObject getMember(HttpServletRequest request){
		TXResponse response = TXResponseFactory.CreateSuccess();

		if(!robot.isOnline()) {
			response = TXResponseFactory.CreateFail(TXErrorCode.SYSTEMRROR);
			return response.getData();
		}
		
		try {
			String type = request.getParameter("type");
			if("group".equals(type)){
				response.put("rows", robot.getContact().getGroupList());
			} else if("normal".equals(type)) {
				response.put("rows", robot.getContact().getMemberList());
			}
			response.put("totalpage",1);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return response.getData();
	}
	
	
	@ResponseBody
	@RequestMapping(value="/sendmessagetoGroup")
	public JSONObject sendmessagetoGroup(HttpServletRequest request,@RequestBody HashMap<String,Object> data){
		TXResponse response = TXResponseFactory.CreateSuccess();

		if(!robot.isOnline()) {
			response = TXResponseFactory.CreateFail(TXErrorCode.SYSTEMRROR);
			return response.getData();
		}
		
		try {
			
			String message = data.get("message").toString();
			@SuppressWarnings("unchecked")
			List<String> groupNameList = (List<String>)data.get("groupnamelist");
			for(int i=0;i<groupNameList.size();i++ ) {
				if(StringUtils.isNotBlank(message) && StringUtils.isNotBlank(groupNameList.get(i)))
					robot.sendMessage(message,groupNameList.get(i));
			}
		
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return response.getData();
	}

	@ResponseBody
	@RequestMapping(value="/getgroup")
	public JSONObject getGroup(HttpServletRequest request){
		TXResponse response = TXResponseFactory.CreateSuccess();

		if(!robot.isOnline()) {
			response = TXResponseFactory.CreateFail(TXErrorCode.SYSTEMRROR);
			return response.getData();
		}
		
		try {
			response.put("rows", robot.getContact().getGroupList());
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return response.getData();
	}
	
	/**
	 * 发送消息
	 * @param data
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/sendmessage")
	public JSONObject testMessage(@RequestBody HashMap<String,Object> data){
		TXResponse response = TXResponseFactory.CreateSuccess();
		
		try {
			if(!robot.isOnline()) {
				response = TXResponseFactory.CreateFail(10, "已下线");
				return response.getData();
			}
			
			String message = data.get("message").toString();
			String username = data.get("username").toString();
			if(StringUtils.isNotBlank(message) && StringUtils.isNotBlank(username)) {
				System.out.println(robot.sendMessage(message,username));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			response = TXResponseFactory.CreateFail(TXErrorCode.SYSTEMRROR);
		}
		return response.getData();
	}
	
}
