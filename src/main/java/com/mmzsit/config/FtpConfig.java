package com.mmzsit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

/**
 * @author ：mmzsit
 * @description：ftp服务器相关配置信息
 * @date ：2018/03/24 14:05
 */
@Getter
@Component
public class FtpConfig {
	/**
	 * ftp服务器地址
	 */
	@Value("${ftp.url}")
	private String url;
	
	/**
	 * ftp服务器端口
	 */
	@Value("${ftp.port}")
	private int port;
	
	/**
	 * ftp服务器用户名
	 */
	@Value("${ftp.username}")
	private String username;
	
	/**
	 * ftp服务器密码
	 */
	@Value("${ftp.password}")
	private String password;
	
	/**
	 * ftp服务器存放文件的路径
	 */
	@Value("${ftp.remotePath}")
	private String remotePath;
	
	/**
	 * 本地需要上传的文件的路径
	 */
	@Value("${ftp.localDir}")
	private String localDir;
	
	/**
	 * 下载文件时，存放在本地的路径
	 */
	@Value("${ftp.downDir}")
	private String downDir;
	
}
