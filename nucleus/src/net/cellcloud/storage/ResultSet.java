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

package net.cellcloud.storage;

import net.cellcloud.exception.StorageException;

/** 结果集。
 * @author Jiangwei Xu
 */
public interface ResultSet {

	/** 游标绝对定位。
	*/
	boolean absolute(int cursor);

	/** 游标相对定位。
	*/
	boolean relative(int cursor);

	/** 游标移动到数据头。
	*/
	boolean first();

	/** 游标移动到数据尾。
	*/
	boolean last();

	/** 游标下移一个数据位。
	*/
	boolean next();

	/** 游标上移一个数据位。
	*/
	boolean previous();

	/** 游标是否在第一个数据位。
	*/
	boolean isFirst();

	/** 游标是否在最后一个数据位。
	*/
	boolean isLast();

	/** 返回指定游标处字符型数据。
	*/
	char getChar(int index);

	/** 返回指定游标标签处字符型数据。
	*/
	char getChar(final String label);

	/** 返回指定游标处整数型数据。
	*/
	int getInt(int index);

	/** 返回指定游标标签处整数型数据。
	*/
	int getInt(final String label);

	/** 返回指定游标处长整数型数据。
	*/
	long getLong(int index);

	/** 返回指定游标标签处长整数型数据。
	*/
	long getLong(final String label);

	/** 返回指定游标处字符串型数据。
	*/
	String getString(int index);

	/** 返回指定游标标签处字符串型数据。
	*/
	String getString(final String label);

	/** 返回指定游标处布尔型数据。
	*/
	boolean getBool(int index);

	/** 返回指定游标标签处布尔型数据。
	*/
	boolean getBool(final String label);

	/** 获取原始数据。
	@return 返回数据长度。
	*/
	byte[] getRaw(final String label, long offset, long length);

	/**
	*/
	void updateChar(int index, char value);

	/**
	*/
	void updateChar(final String label, char value);

	/**
	*/
	void updateInt(int index, int value);

	/**
	*/
	void updateInt(final String label, int value);

	/**
	*/
	void updateLong(int index, long value);

	/**
	*/
	void updateLong(final String label, long value);

	/**
	*/
	void updateString(int index, final String value);

	/**
	*/
	void updateString(final String label, final String value);

	/**
	*/
	void updateBool(int index, boolean value);

	/**
	*/
	void updateBool(final String label, boolean value);

	/**
	*/
	void updateRaw(final String label, byte[] src, int offset, int length)
			throws StorageException;

	/**
	*/
	void updateRaw(final String label, byte[] src, long offset, long length)
			throws StorageException;
}
