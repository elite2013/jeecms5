package com.jeecms.cms.action.front;

import static com.jeecms.cms.Constants.TPLDIR_SPECIAL;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.jeecms.cms.entity.assist.CmsGuestbook;
import com.jeecms.cms.entity.assist.CmsGuestbookCtg;
import com.jeecms.cms.entity.assist.CmsGuestbookExt;
import com.jeecms.cms.entity.main.CmsSite;
import com.jeecms.cms.entity.main.CmsUser;
import com.jeecms.cms.manager.assist.CmsGuestbookCtgMng;
import com.jeecms.cms.manager.assist.CmsGuestbookMng;
import com.jeecms.cms.web.CmsUtils;
import com.jeecms.cms.web.FrontUtils;
import com.jeecms.common.util.Msg;
import com.jeecms.common.web.RequestUtils;
import com.jeecms.common.web.ResponseUtils;
import com.jeecms.common.web.session.SessionProvider;
import com.octo.captcha.service.CaptchaServiceException;
import com.octo.captcha.service.image.ImageCaptchaService;

@Controller
public class GuestbookAct {
	private static final Logger log = LoggerFactory
			.getLogger(GuestbookAct.class);

	public static final String GUESTBOOK_INDEX = "guestbookIndex";
	public static final String GUESTBOOK_CTG = "guestbookCtg";
	public static final String GUESTBOOK_DETAIL = "guestbookDetail";

	/**
	 * 留言板首页或类别页
	 * 
	 * @param ctgId
	 *            留言类别
	 * @param request
	 * @param response
	 * @param model
	 * @return
	 */
	@RequestMapping(value = "/guestbook*.jspx", method = RequestMethod.GET)
	public String index(Integer ctgId, HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		CmsSite site = CmsUtils.getSite(request);
		FrontUtils.frontData(request, model, site);
		FrontUtils.frontPageData(request, model);
		CmsGuestbookCtg ctg = null;
		if (ctgId != null) {
			ctg = cmsGuestbookCtgMng.findById(ctgId);
		}
		if (ctg == null) {
			// 留言板首页
			return FrontUtils.getTplPath(request, site.getSolutionPath(),
					TPLDIR_SPECIAL, GUESTBOOK_INDEX);
		} else {
			// 留言板类别页
			model.addAttribute("ctg", ctg);
			return FrontUtils.getTplPath(request, site.getSolutionPath(),
					TPLDIR_SPECIAL, GUESTBOOK_CTG);
		}
	}

	@RequestMapping(value = "/guestbook/{id}.jspx", method = RequestMethod.GET)
	public String detail(@PathVariable Integer id, HttpServletRequest request,
			HttpServletResponse response, ModelMap model) {
		CmsSite site = CmsUtils.getSite(request);
		CmsGuestbook guestbook = null;
		if (id != null) {
			guestbook = cmsGuestbookMng.findById(id);
		}
		model.addAttribute("guestbook", guestbook);
		FrontUtils.frontData(request, model, site);
		return FrontUtils.getTplPath(request, site.getSolutionPath(),
				TPLDIR_SPECIAL, GUESTBOOK_DETAIL);

	}

	/**
	 * 提交留言。ajax提交。
	 * 
	 * @param contentId
	 * @param pageNo
	 * @param request
	 * @param response
	 * @param model
	 * @throws JSONException
	 */
	@RequestMapping(value = "/guestbook.jspx", method = RequestMethod.POST)
	public void submit(Integer siteId, Integer ctgId, String title,
			String content, String email, String phone, String qq,
			String captcha, HttpServletRequest request,
			HttpServletResponse response, ModelMap model) throws JSONException {
		CmsSite site = CmsUtils.getSite(request);
		CmsUser member = CmsUtils.getUser(request);
		Msg msg = new Msg();
		msg.setSuccess(false);
		msg.setStatus("1");
		if (siteId == null) {
			siteId = site.getId();
		}
		JSONObject json = new JSONObject();
		try {
			if (!imageCaptchaService.validateResponseForID(session
					.getSessionId(request, response), captcha)) {
				msg.setTitle("验证码错误");
				JSONObject jsonObject = JSONObject.fromObject(msg);
				ResponseUtils.renderJson(response, jsonObject.toString());
				return;
			}
		} catch (CaptchaServiceException e) {
			msg.setTitle("验证码错误");
			JSONObject jsonObject = JSONObject.fromObject(msg);
			ResponseUtils.renderJson(response, jsonObject.toString());
			return;
		}
		String ip = RequestUtils.getIpAddr(request);
		cmsGuestbookMng.save(member, siteId, ctgId, ip, title, content, email,
				phone, qq);

		msg.setSuccess(true);
		msg.setStatus("0");
		msg.setTitle("咨询添加成功");
		JSONObject jsonObject = JSONObject.fromObject(msg);
		ResponseUtils.renderJson(response, jsonObject.toString());
	}

	/**
	 * 回复咨询问题。
	 * 
	 * @param contentId
	 * @param pageNo
	 * @param request
	 * @param response
	 * @param model
	 * @throws JSONException
	 */
	@RequestMapping(value = "/replyGuestbook.jspx", method = RequestMethod.POST)
	public void reply( Integer gBookId,
			String reply,	String captcha, HttpServletRequest request,
			HttpServletResponse response, ModelMap model) throws JSONException {
		CmsSite site = CmsUtils.getSite(request);
		CmsUser member = CmsUtils.getUser(request);
		Msg msg = new Msg();
		msg.setSuccess(false);
		msg.setStatus("1");
		CmsGuestbook book = null;
		CmsGuestbookExt ext = null;
		JSONObject json = new JSONObject();
		try {

			
			if (!imageCaptchaService.validateResponseForID(session
					.getSessionId(request, response), captcha)) {
				msg.setTitle("验证码错误");
				JSONObject jsonObject = JSONObject.fromObject(msg);
				ResponseUtils.renderJson(response, jsonObject.toString());
				return;
			}
			
			if(gBookId !=null && gBookId>0){
				book = cmsGuestbookMng.findById(gBookId);
				book.setAdmin(member);
				book.setReplayTime(new Date());
				book.setChecked(true);
				ext = book.getExt();
				ext.setReply(reply);
			}
			
			
		} catch (CaptchaServiceException e) {
			msg.setTitle("验证码错误");
			JSONObject jsonObject = JSONObject.fromObject(msg);
			ResponseUtils.renderJson(response, jsonObject.toString());
			return;
		}
		String ip = RequestUtils.getIpAddr(request);
		cmsGuestbookMng.save(book, ext, book.getCtg().getId(), ip);

		msg.setSuccess(true);
		msg.setStatus("0");
		msg.setTitle("咨询回复添加成功");
		JSONObject jsonObject = JSONObject.fromObject(msg);
		ResponseUtils.renderJson(response, jsonObject.toString());
	}

	@Autowired
	private CmsGuestbookCtgMng cmsGuestbookCtgMng;
	@Autowired
	private CmsGuestbookMng cmsGuestbookMng;
	@Autowired
	private SessionProvider session;
	@Autowired
	private ImageCaptchaService imageCaptchaService;

}
