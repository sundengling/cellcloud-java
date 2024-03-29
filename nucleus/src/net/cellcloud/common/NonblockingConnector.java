/*
-----------------------------------------------------------------------------
This source file is part of Cell Cloud.

Copyright (c) 2009-2012 Cell Cloud Team (cellcloudproject@gmail.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
-----------------------------------------------------------------------------
*/

package net.cellcloud.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.cellcloud.core.Logger;

/** 非阻塞式网络连接器。
 * 
 * @author Jiangwei Xu
 */
public class NonblockingConnector extends MessageService implements
	MessageConnector {

	protected static final int BLOCK = 8192;

	private InetSocketAddress address;
	private long connectTimeout;
	private SocketChannel channel;
	private Selector selector;

	private Session session;

	private Thread handleThread;
	private boolean spinning = false;
	private boolean running = false;

	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffer;
	// 待发送消息列表
	private Vector<Message> messages;

	public NonblockingConnector() {
		this.connectTimeout = 10000;
		this.readBuffer = ByteBuffer.allocate(BLOCK);
		this.writeBuffer = ByteBuffer.allocate(BLOCK);
		this.messages = new Vector<Message>();
	}

	/** 返回连接地址。
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	@Override
	public boolean connect(InetSocketAddress address) {
		if (this.channel != null && this.channel.isConnected()) {
			Logger.w(NonblockingConnector.class, "Connector has connected to " + address.getAddress().getHostAddress());
			return true;
		}

		if (this.running && null != this.channel) {
			this.spinning = false;

			try {
				if (this.channel.isOpen()) {
					this.channel.close();
				}

				if (null != this.selector) {
					this.selector.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			while (this.running) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}

			Thread.yield();
		}

		this.address = address;

		try {
			this.channel = SocketChannel.open();
			this.channel.configureBlocking(false);

			// 配置
			/* 以下为 JDK7 的代码
			this.channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
			this.channel.setOption(StandardSocketOptions.SO_RCVBUF, BLOCK);
			this.channel.setOption(StandardSocketOptions.SO_SNDBUF, BLOCK);
			*/
			// 以下为 JDK6 的代码
			this.channel.socket().setKeepAlive(true);
			this.channel.socket().setReceiveBufferSize(BLOCK);
			this.channel.socket().setSendBufferSize(BLOCK);

			this.selector = Selector.open();
			// 注册事件
			this.channel.register(this.selector, SelectionKey.OP_CONNECT);

			// 连接
			this.channel.connect(this.address);
		} catch (IOException e) {
			Logger.e(NonblockingConnector.class, e.getMessage());

			try {
				if (null != this.channel) {
					this.channel.close();
				}
			} catch (Exception e1) {
				// Nothing
			}
			try {
				if (null != this.selector) {
					this.selector.close();
				}
			} catch (Exception e1) {
				// Nothing
			}

			return false;
		}

		// 创建 Session
		this.session = new Session(this, this.address);

		this.handleThread = new Thread() {

			@Override
			public void run() {
				running = true;

				// 通知 Session 创建。
				fireSessionCreated();

				try {
					loopDispatch();
				} catch (Exception e) {
					spinning = false;
				}

				// 通知 Session 销毁。
				fireSessionDestroyed();

				running = false;

				try {
					if (null != channel)
						channel.close();
					if (null != selector)
						selector.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		this.handleThread.setName("NonblockingConnector@" + this.address.getAddress().getHostAddress() + ":" + this.address.getPort());
		// 启动线程
		this.handleThread.start();

		return true;
	}

	@Override
	public void disconnect() {
		if (null != this.channel) {
			this.spinning = false;

			if (this.channel.isConnected()) {
				fireSessionClosed();
			}

			try {
				if (this.channel.isOpen()) {
					this.channel.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (null != this.selector) {
			if (this.selector.isOpen()) {
				try {
					this.selector.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		while (this.running) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		this.channel = null;
	}

	@Override
	public void setConnectTimeout(long timeout) {
		this.connectTimeout = timeout;
	}

	public long getConnectTimeout() {
		return this.connectTimeout;
	}

	/** 是否已连接。
	 */
	public boolean isConnected() {
		return (null != this.channel && this.channel.isConnected());
	}

	@Override
	public Session getSession() {
		return this.session;
	}

	public void write(Message message) {
		this.write(null, message);
	}

	@Override
	public void write(Session session, Message message) {
		this.messages.add(message);
	}

	@Override
	public void read(Message message, Session session) {
		// Nothing
	}

	private void fireSessionCreated() {
		if (null != this.handler) {
			this.handler.sessionCreated(this.session);
		}
	}
	private void fireSessionOpened() {
		if (null != this.handler) {
			this.handler.sessionOpened(this.session);
		}
	}
	private void fireSessionClosed() {
		if (null != this.handler) {
			this.handler.sessionClosed(this.session);
		}
	}
	private void fireSessionDestroyed() {
		if (null != this.handler) {
			this.handler.sessionDestroyed(this.session);
		}
	}
	private void fireErrorOccurred(int errorCode) {
		if (null != this.handler) {
			this.handler.errorOccurred(errorCode, this.session);
		}
	}

	/** 事件循环。 */
	private void loopDispatch() throws IOException {
		// 自旋
		this.spinning = true;

		while (this.spinning) {
			while (this.selector.select(this.connectTimeout) > 0) {
				Set<SelectionKey> keys = this.selector.selectedKeys();
				Iterator<SelectionKey> it = keys.iterator();
				while (it.hasNext()) {
					SelectionKey key = (SelectionKey) it.next();
					it.remove();

					// 当前通道选择器产生连接已经准备就绪事件，并且客户端套接字通道尚未连接到服务端套接字通道
					if (key.isConnectable()) {
						if (!doConnect(key)) {
							this.spinning = false;
							return;
						}
					}
					else if (key.isReadable()) {
						receive(key);
					}
					else if (key.isWritable()) {
						send(key);
					}
				} //# while

				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} //# while

			Thread.yield();

		} // # while
	}

	private boolean doConnect(SelectionKey key) {
		// 获取创建通道选择器事件键的套接字通道
		SocketChannel channel = (SocketChannel)key.channel();

		// 判断此通道上是否正在进行连接操作。  
        // 完成套接字通道的连接过程。
		if (channel.isConnectionPending()) {
			try {
				channel.finishConnect();
			} catch (IOException e) {
//				e.printStackTrace();

				try {
					this.channel.close();
					this.selector.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				// 连接失败
				fireErrorOccurred(MessageHandler.EC_CONNECT_FAILED);
				return false;
			}

			// 连接成功，打开 Session
			fireSessionOpened();
		}

		try {
			channel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		} catch (ClosedChannelException e) {
			e.printStackTrace();
		}

		return true;
	}

	private void receive(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();

		if (!channel.isConnected()) {
			return;
		}

		int read = 0;
		do {
			try {
				read = channel.read(this.readBuffer);
			} catch (IOException e) {
				fireSessionClosed();

				try {
					this.channel.close();
					this.selector.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				return;
			}

			if (read == 0) {
				break;
			}
			else if (read == -1) {
				fireSessionClosed();

				try {
					this.channel.close();
					this.selector.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				return;
			}

			this.readBuffer.flip();

			byte[] array = new byte[read];
			this.readBuffer.get(array);

			process(array);

			this.readBuffer.clear();
		} while (read > 0);

		try {
			channel.register(this.selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void send(SelectionKey key) {
		try {
			SocketChannel channel = (SocketChannel) key.channel();

			if (!channel.isConnected()) {
				fireSessionClosed();
				return;
			}

			if (!this.messages.isEmpty()) {
				// 有消息，进行发送

				Message message = null;
				for (int i = 0, len = this.messages.size(); i < len; ++i) {
					message = this.messages.remove(0);

					if (this.existDataMark()) {
						byte[] data = message.get();
						byte[] head = this.getHeadMark();
						byte[] tail = this.getTailMark();
						byte[] pd = new byte[data.length + head.length + tail.length];
						System.arraycopy(head, 0, pd, 0, head.length);
						System.arraycopy(data, 0, pd, head.length, data.length);
						System.arraycopy(tail, 0, pd, head.length + data.length, tail.length);
						this.writeBuffer.put(pd);
					}
					else {
						this.writeBuffer.put(message.get());
					}

					this.writeBuffer.flip();

					channel.write(this.writeBuffer);

					this.writeBuffer.clear();

					if (null != this.handler) {
						this.handler.messageSent(this.session, message);
					}
				}
			}

			try {
				// 注册
				channel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			} catch (ClosedChannelException e1) {
				// Nothing
			}
		} catch (IOException e) {
			e.printStackTrace();
			Logger.e(NonblockingConnector.class, e.getMessage());
		}
	}

	private void process(byte[] data) {
		// 根据数据标志获取数据
		if (this.existDataMark()) {
			byte[] headMark = this.getHeadMark();
			byte[] tailMark = this.getTailMark();

			int cursor = 0;
			int length = data.length;
			boolean head = false;
			boolean tail = false;
			byte[] buf = new byte[8192];
			int bufIndex = 0;

			while (cursor < length) {
				head = true;
				tail = true;

				byte b = data[cursor];

				// 判断是否是头标识
				if (b == headMark[0]) {
					for (int i = 1, len = headMark.length; i < len; ++i) {
						if (data[cursor + i] != headMark[i]) {
							head = false;
							break;
						}
					}
				}
				else {
					head = false;
				}

				// 判断是否是尾标识
				if (b == tailMark[0]) {
					for (int i = 1, len = tailMark.length; i < len; ++i) {
						if (data[cursor + i] != tailMark[i]) {
							tail = false;
							break;
						}
					}
				}
				else {
					tail = false;
				}

				if (head) {
					// 遇到头标识，开始记录数据
					cursor += headMark.length;
					bufIndex = 0;
					buf[bufIndex] = data[cursor];
				}
				else if (tail) {
					// 遇到尾标识，提取 buf 内数据
					byte[] pdata = new byte[bufIndex + 1];
					System.arraycopy(buf, 0, pdata, 0, bufIndex + 1);
					Message message = new Message(pdata);
					if (null != this.handler) {
						this.handler.messageReceived(this.session, message);
					}

					cursor += tailMark.length;
				}
				else {
					++bufIndex;
					buf[bufIndex] = b;
				}

				// 下一个字节
				++cursor;
			}

			buf = null;
		}
		else {
			Message message = new Message(data);
			if (null != this.handler) {
				this.handler.messageReceived(this.session, message);
			}
		}
	}
}
