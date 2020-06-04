/*
 * Copyright 2011 Fernando Ribeiro
 * 
 * This file is part of Progress Sonic ESB Utils.
 *
 * Progress Sonic ESB Utils is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Progress Sonic ESB Utils is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Progress Sonic ESB Utils. If not, see <http://www.gnu.org/licenses/>.
 */
package com.progress.codeshare.esbextension.count;

import net.sf.saxon.expr.XPathContext;

public class CountExtension {
	private long count;
	private long initCount;

	public CountExtension(long initCount) {
		count = initCount;

		this.initCount = initCount;
	}

	public long getCount(XPathContext ctx) {
		return count;
	}

	public long next(XPathContext ctx) {
		return next(ctx, false);
	}

	public long next(XPathContext ctx, boolean resetFlag) {

		/* Ensure that Long.MAX_VALUE is higher than the count */
		if (Long.MAX_VALUE > count)
			++count;
		else
			count = initCount;

		return count;
	}

	public long previous(XPathContext ctx) {
		return previous(ctx, false);
	}

	public long previous(XPathContext ctx, boolean resetFlag) {

		/* Ensure that initCount is lower than the count */
		if (initCount < count)
			--count;
		else
			count = initCount;

		return count;
	}

}