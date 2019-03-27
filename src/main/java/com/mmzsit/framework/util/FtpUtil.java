package com.mmzsit.framework.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.SocketException;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic="文件上传/下载===ftp服务器:")
public class FtpUtil {
	private static FTPClient mFTPClient = new FTPClient();
	private static FtpUtil ftp = new FtpUtil();
	
	public FtpUtil() {
		// 在控制台打印操作过程
		 mFTPClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
	}


	/**
	 * 上传文件到ftp服务器
	 */
	public static boolean ftpUpload(String fileName, String ftpUrl, int ftpPort,
			String ftpUsername, String ftpPassword, String ftpLocalDir, String ftpRemotePath) {
		boolean result = false;
		try {
			boolean isConnection = ftp.openConnection(ftpUrl, ftpPort, ftpUsername, ftpPassword);
			if (isConnection) {
				boolean isSuccess = ftp.upload(ftpRemotePath, ftpLocalDir + "/" + fileName);
				if (isSuccess) {
					log.info("文件上传成功！");
					result = true;
				} else {
					log.info("文件上传失败！");
					result = false;
				}
				ftp.logout();
			} else {
				log.info("链接ftp服务器失败，请检查配置信息是否正确！");
				result = false;
			}

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 从ftp服务器下载文件到本地
	 */
	public static boolean ftpDownload(String fileName, String ftpUrl, int ftpPort,
			String ftpUsername, String ftpPassword, String ftpRemotePath, String ftpDownDir) {
		boolean result = false;
		try {
			boolean isConnection = ftp.openConnection(ftpUrl, ftpPort, ftpUsername, ftpPassword);
			if (isConnection) {
				boolean isDownloadOk = ftp.downLoad(fileName, ftpDownDir);
				boolean isCreateOk = ftp.createDirectory(ftpRemotePath, ftp.mFTPClient);
				if (isDownloadOk && isCreateOk) {
					log.info("文件下载成功！");
					result = true;
				} else {
					log.info("文件下载失败！");
					result = false;
				}
				ftp.logout();
			} else {
				log.info("链接ftp服务器失败，请检查配置信息是否正确！");
				result = false;
			}

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;

	}

	/**
	 * 连接ftp服务器
	 * 
	 * @param host
	 *            ip地址
	 * @param port
	 *            端口号
	 * @param account
	 *            账号
	 * @param pwd
	 *            密码
	 * @return 是否连接成功
	 * @throws SocketException
	 * @throws IOException
	 */
	private boolean openConnection(String host, int port, String account, String pwd)
			throws SocketException, IOException {
		mFTPClient.setControlEncoding("UTF-8");
		mFTPClient.connect(host, port);

		if (FTPReply.isPositiveCompletion(mFTPClient.getReplyCode())) {
			mFTPClient.login(account, pwd);
			if (FTPReply.isPositiveCompletion(mFTPClient.getReplyCode())) {
				System.err.println(mFTPClient.getSystemType());
				FTPClientConfig config = new FTPClientConfig(mFTPClient.getSystemType().split(" ")[0]);
				config.setServerLanguageCode("zh");
				mFTPClient.configure(config);
				return true;
			}
		}
		disConnection();
		return false;
	}

	/**
	 * 登出并断开连接
	 */
	public void logout() {
		System.err.println("logout");
		if (mFTPClient.isConnected()) {
			System.err.println("logout");
			try {
				mFTPClient.logout();
				disConnection();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 断开连接
	 */
	private void disConnection() {
		if (mFTPClient.isConnected()) {
			try {
				mFTPClient.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 下载文件到本地地址
	 * 
	 * @param remotePath
	 *            远程地址
	 * @param loacal
	 *            本地地址
	 * @throws IOException
	 */
	public boolean downLoad(String remotePath, String localDir) throws IOException {
		// 进入被动模式
		mFTPClient.enterLocalPassiveMode();
		// 以二进制进行传输数据
		mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);
		FTPFile[] ftpFiles = mFTPClient.listFiles(remotePath);
		if (ftpFiles == null || ftpFiles.length == 0) {
			log.info("远程文件不存在");
			return false;
		} else if (ftpFiles.length > 1) {
			log.info("远程文件是文件夹");
			return false;
		}
		long lRemoteSize = ftpFiles[0].getSize();
		// 本地文件的地址
		File localFileDir = new File(localDir);
		if (!localFileDir.exists()) {
			localFileDir.mkdirs();
		}
		File localFile = new File(localFileDir, ftpFiles[0].getName());
		long localSize = 0;
		FileOutputStream fos = null;
		if (localFile.exists()) {
			if (localFile.length() == lRemoteSize) {
				System.err.println("已经下载完毕");
				return true;
			} else if (localFile.length() < lRemoteSize) {
				// 要下载的文件存在，进行断点续传
				localSize = localFile.length();
				mFTPClient.setRestartOffset(localSize);
				fos = new FileOutputStream(localFile, true);
			}
		}
		if (fos == null) {
			fos = new FileOutputStream(localFile);
		}
		InputStream is = mFTPClient.retrieveFileStream(remotePath);
		byte[] buffers = new byte[1024];
		long step = lRemoteSize / 10;
		long process = localSize / step;
		int len = -1;
		while ((len = is.read(buffers)) != -1) {
			fos.write(buffers, 0, len);
			localSize += len;
			long newProcess = localSize / step;
			if (newProcess > process) {
				process = newProcess;
				System.err.println("下载进度:" + process);
			}
		}
		is.close();
		fos.close();
		boolean isDo = mFTPClient.completePendingCommand();
		if (isDo) {
			System.err.println("下载成功");
		} else {
			System.err.println("下载失败");
		}
		return isDo;

	}

	/**
	 * 创建远程目录
	 * 
	 * @param remote
	 *            远程目录
	 * @param ftpClient
	 *            ftp客户端
	 * @return 是否创建成功
	 * @throws IOException
	 */
	public boolean createDirectory(String remote, FTPClient ftpClient) throws IOException {
		String dirctory = remote.substring(0, remote.lastIndexOf("/") + 1);
		if (!dirctory.equalsIgnoreCase("/") && !ftpClient.changeWorkingDirectory(dirctory)) {
			int start = 0;
			int end = 0;
			if (dirctory.startsWith("/")) {
				start = 1;
			}
			end = dirctory.indexOf("/", start);
			while (true) {
				String subDirctory = remote.substring(start, end);
				if (!ftpClient.changeWorkingDirectory(subDirctory)) {
					if (ftpClient.makeDirectory(subDirctory)) {
						ftpClient.changeWorkingDirectory(subDirctory);
					} else {
						System.err.println("创建目录失败");
						return false;
					}
				}
				start = end + 1;
				end = dirctory.indexOf("/", start);
				if (end <= start) {
					break;
				}
			}
		}
		return true;
	}

	/**
	 * 上传的文件
	 * 
	 * @param remotePath
	 *            上传文件的路径地址（文件夹地址）
	 * @param localPath
	 *            本地文件的地址
	 * @throws IOException
	 *             异常
	 */
	public boolean upload(String remotePath, String localPath) throws IOException {
		// 进入被动模式
		mFTPClient.enterLocalPassiveMode();
		// 以二进制进行传输数据
		mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);
		File localFile = new File(localPath);
		if (!localFile.exists()) {
			System.err.println("本地文件不存在");
			return false;
		}
		String fileName = localFile.getName();
		if (remotePath.contains("/")) {
			boolean isCreateOk = createDirectory(remotePath, mFTPClient);
			if (!isCreateOk) {
				System.err.println("文件夹创建失败");
				return false;
			}
		}

		// 列出ftp服务器上的文件
		FTPFile[] ftpFiles = mFTPClient.listFiles(remotePath);
		long remoteSize = 0l;
		String remoteFilePath = remotePath + "/" + fileName;
		if (ftpFiles.length > 0) {
			FTPFile mFtpFile = null;
			for (FTPFile ftpFile : ftpFiles) {
				if (ftpFile.getName().endsWith(fileName)) {
					mFtpFile = ftpFile;
					break;
				}
			}
			if (mFtpFile != null) {
				remoteSize = mFtpFile.getSize();
				if (remoteSize == localFile.length()) {
					System.err.println("文件已经上传成功");
					return true;
				}
				if (remoteSize > localFile.length()) {
					if (!mFTPClient.deleteFile(remoteFilePath)) {
						System.err.println("服务端文件操作失败");
					} else {
						boolean isUpload = uploadFile(remoteFilePath, localFile, 0);
						System.err.println("是否上传成功：" + isUpload);
					}
					return true;
				}
				if (!uploadFile(remoteFilePath, localFile, remoteSize)) {
					System.err.println("文件上传成功");
					return true;
				} else {
					// 断点续传失败删除文件，重新上传
					if (!mFTPClient.deleteFile(remoteFilePath)) {
						System.err.println("服务端文件操作失败");
					} else {
						boolean isUpload = uploadFile(remoteFilePath, localFile, 0);
						System.err.println("是否上传成功：" + isUpload);
					}
					return true;
				}
			}
		}

		boolean isUpload = uploadFile(remoteFilePath, localFile, remoteSize);
		System.err.println("是否上传成功：" + isUpload);
		return isUpload;
	}

	/**
	 * 上传文件
	 * 
	 * @param remoteFile
	 *            包含文件名的地址
	 * @param localFile
	 *            本地文件
	 * @param remoteSize
	 *            服务端已经存在的文件大小
	 * @return 是否上传成功
	 * @throws IOException
	 */
	private boolean uploadFile(String remoteFile, File localFile, long remoteSize) throws IOException {
		long step = localFile.length() / 10;
		long process = 0;
		long readByteSize = 0;
		RandomAccessFile randomAccessFile = new RandomAccessFile(localFile, "r");
		OutputStream os = mFTPClient.appendFileStream(remoteFile);
		if (remoteSize > 0) {
			// 已经上传一部分的时候就要进行断点续传
			process = remoteSize / step;
			readByteSize = remoteSize;
			randomAccessFile.seek(remoteSize);
			mFTPClient.setRestartOffset(remoteSize);
		}
		byte[] buffers = new byte[1024];
		int len = -1;
		while ((len = randomAccessFile.read(buffers)) != -1) {
			os.write(buffers, 0, len);
			readByteSize += len;
			long newProcess = readByteSize / step;
			if (newProcess > process) {
				process = newProcess;
				System.err.println("当前上传进度为：" + process);
			}
		}
		os.flush();
		randomAccessFile.close();
		os.close();
		boolean result = mFTPClient.completePendingCommand();
		return result;
	}

}