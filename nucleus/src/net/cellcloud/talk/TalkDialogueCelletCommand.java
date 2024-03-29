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

import java.io.ByteArrayInputStream;

import net.cellcloud.common.Packet;
import net.cellcloud.common.Session;
import net.cellcloud.core.Logger;
import net.cellcloud.talk.stuff.Primitive;
import net.cellcloud.util.Util;

/** Talk dialogue cellet
 * 
 * @author Jiangwei Xu
 */
public final class TalkDialogueCelletCommand extends TalkCommand {

	public TalkDialogueCelletCommand(TalkService service, Session session,
			Packet packet) {
		super(service, session, packet);
	}

	@Override
	public void execute() {
		// 包格式：序列化的原语|源标签

		if (this.packet.getSubsegmentNumber() < 2) {
			Logger.e(TalkDialogueCelletCommand.class, "Dialogue packet format error");
			return;
		}

		byte[] pridata = this.packet.getSubsegment(0);
		ByteArrayInputStream stream = new ByteArrayInputStream(pridata);

		byte[] tagdata = this.packet.getSubsegment(1);
		String talkerTag = Util.bytes2String(tagdata);

		// 反序列化原语
		Primitive primitive = new Primitive(talkerTag);
		primitive.read(stream);

		this.service.dialogueCellet(this.session, talkerTag, primitive);
	}
}
