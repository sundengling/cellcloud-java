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

package net.cellcloud.talk;

import net.cellcloud.common.Message;
import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.util.Util;

/** Talk request cellet command
 * 
 * @author Jiangwei Xu
 */
public final class TalkRequestCelletCommand extends TalkCommand {

	public TalkRequestCelletCommand(TalkService service, Session session,
			Packet packet) {
		super(service, session, packet);
	}

	@Override
	public void execute() {
		// 包格式：Cellet标识串|请求方标签

		byte[] identifier = this.packet.getSubsegment(0);
		byte[] talkTag = this.packet.getSubsegment(1);

		// 包格式：请求方标签|成功码|Cellet识别串|Cellet版本
		Packet packet = new Packet(TalkPacketDefine.TPT_REQUEST, 1);
		// 请求方标签
		packet.appendSubsegment(talkTag);

		// 请求 Cellet
		TalkTracker tracker = this.service.requestCellet(this.session,
				Util.bytes2String(talkTag), Util.bytes2String(identifier));

		if (null != tracker && null != tracker.activeCellet) {
			// 成功码
			packet.appendSubsegment(TalkCommand.SC_SUCCESSFUL);
			// Cellet识别串
			packet.appendSubsegment(identifier);
			// Cellet版本
			String ret = tracker.activeCellet.getFeature().getVersion().toString();
			packet.appendSubsegment(Util.string2Bytes(ret));
		}
		else {
			packet.appendSubsegment(TalkCommand.SC_FAILED_NOCELLET);
		}

		// 打包数据
		byte[] data = Packet.pack(packet);
		if (null != data) {
			Message message = new Message(data);
			this.session.write(message);
		}
	}
}
