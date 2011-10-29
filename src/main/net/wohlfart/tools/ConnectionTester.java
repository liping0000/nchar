package net.wohlfart.tools;

import com.mchange.v2.c3p0.impl.DefaultConnectionTester;

/*
 * extending c3p0DataSource's default connection tester, this
 * class is used in the spring-beans config for setting up
 * the c3p0DataSource
 * 
 * TODO: implement a custom connection check here
 * 
 * @author Michael Wohlfart
 * 
 */
public class ConnectionTester extends DefaultConnectionTester {


}
