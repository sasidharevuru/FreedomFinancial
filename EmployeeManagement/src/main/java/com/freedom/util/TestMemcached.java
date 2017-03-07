/**
 * Copyright (c) 2008 Greg Whalin
 * All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the BSD license
 *
 * This library is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.
 *
 * You should have received a copy of the BSD License along with this
 * library.
 *
 * @author greg whalin <greg@meetup.com> 
 */
package com.freedom.util;

public class TestMemcached {
	
	private static MemcachedClient mcc = null;
	
	public static MemcachedClient getMemcachedclinet()
	{
		if(mcc != null)
		{
		}
		else
		{
			SockIOPool pool = SockIOPool.getInstance("Test1");
			String[] servers = {"localhost:11211"};
	        pool.setServers( servers );
	        pool.setFailover( true );
	        pool.setInitConn( 10 );
	        pool.setMinConn( 5 );
	        pool.setMaxConn( 250 );
	        pool.setMaintSleep( 30 );
	        pool.setNagle( false );
	        pool.setSocketTO( 3000 );
	        pool.setAliveCheck( true );
	        pool.initialize();
			mcc = new MemcachedClient("Test1");
			
		}	
		return mcc;
	}
 
}
