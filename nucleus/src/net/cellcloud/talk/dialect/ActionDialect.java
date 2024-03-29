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

package net.cellcloud.talk.dialect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.cellcloud.talk.TalkService;
import net.cellcloud.talk.stuff.ObjectiveStuff;
import net.cellcloud.talk.stuff.PredicateStuff;
import net.cellcloud.talk.stuff.Primitive;
import net.cellcloud.talk.stuff.SubjectStuff;

/** 动作方言。
 * 
 * @author Jiangwei Xu
 */
public final class ActionDialect extends Dialect {

	private String action;
	private HashMap<String, String> params;

	public ActionDialect(String tracker) {
		super(ActionDialectFactory.FACTORY_NAME, tracker);
		this.params = new HashMap<String, String>();
	}

	@Override
	public Primitive translate(final String tag) {
		if (null == this.action || this.action.isEmpty()) {
			return null;
		}

		String name = null;
		String value = null;

		Primitive primitive = new Primitive(tag);

		Iterator<String> iter = this.params.keySet().iterator();
		while (iter.hasNext()) {
			name = iter.next();
			value = this.params.get(name);

			SubjectStuff nameStuff = new SubjectStuff(name);
			ObjectiveStuff valueStuff = new ObjectiveStuff(value);
			primitive.commit(nameStuff);
			primitive.commit(valueStuff);
		}

		PredicateStuff actionStuff = new PredicateStuff(this.action);
		primitive.commit(actionStuff);

		primitive.capture(this);

		return primitive;
	}

	@Override
	public void build(Primitive primitive) {
		this.action = primitive.predicates().get(0).getValueAsString();

		if (null != primitive.subjects()) {
			List<SubjectStuff> names = primitive.subjects();
			List<ObjectiveStuff> values = primitive.objectives();
			for (int i = 0, size = names.size(); i < size; ++i) {
				this.params.put(names.get(i).getValueAsString(), values.get(i).getValueAsString());
			}
		}
	}

	/** 设置动作名。
	 */
	public void setAction(final String action) {
		this.action = action;
	}

	/** 返回动作名。
	 */
	public String getAction() {
		return this.action;
	}

	public void appendParam(final String name, final String value) {
		this.params.put(name, value);
	}
	public void appendParam(final String name, final int value) {
		this.params.put(name, Integer.toString(value));
	}
	public void appendParam(final String name, final long value) {
		this.params.put(name, Long.toString(value));
	}
	public void appendParam(final String name, final boolean value) {
		this.params.put(name, Boolean.toString(value));
	}

	public String getParamAsString(final String name) {
		return this.params.get(name);
	}
	public int getParamAsInt(final String name) {
		if (this.params.containsKey(name))
			return Integer.parseInt(this.params.get(name));
		else
			return 0;
	}
	public long getParamAsLong(final String name) {
		if (this.params.containsKey(name))
			return Long.parseLong(this.params.get(name));
		else
			return 0;
	}
	public boolean getParamAsBoolean(final String name) {
		if (this.params.containsKey(name))
			return Boolean.parseBoolean(this.params.get(name));
		else
			return false;
	}

	/** 判断指定名称的参数是否存在。
	 */
	public boolean existParam(final String name) {
		return this.params.containsKey(name);
	}

	/** 执行动作委派（异步）。
	 */
	public void act(ActionDelegate delegate) {
		TalkService.getInstance().doAction(this, delegate);
	}
}
