Copyright (c) 2025 Intercontinental Exchange, ICE Data Services. All rights reserved.
=====================================================================================
JStandard API SDK 01.031.001
=====================================================================================
ICE Data Services receives market data from more than 500 global venues.
ICE Data Services normalizes this data and makes it available to clients within 
a single consolidated feed. The JStandard API is a Java application programming
interface (API) that enables developers to request and receive data from the
consolidated feed. Using this API, developers can leverage ICE Data Services'
direct connections to access data from exchanges all over the world.

The API provides the following capabilities:

   - Real Time & Delayed Snapshot Market Data (Lvl1)
   - Real Time & Delayed Streaming Market Data (Lvl1/Lvl2)
   - History (Tick)
   - History (Bar Data) - Intraday/Daily/Weekly/Monthly
   - News Headlines and Stories
   - API-Calculated Greeks & Options


CONTENTS
========

1. PURPOSE OF THIS RELEASE
2. SYSTEM REQUIREMENTS
3. DEPENDENCIES
4. PACKAGE STRUCTURE
5. WHERE SHOULD I START
6. WHAT IF I NEED HELP
7. MIGRATION
8. ATTRIBUTION AND LICENSES FOR CERTAIN SOFTWARE COMPONENTS
9. CHANGE LOG


1. PURPOSE OF THIS RELEASE:
===========================

- Support Factset snapshot requests.


2. SYSTEM REQUIREMENTS:
=======================

Refer to the API Compatibility Matrix on the Developer Center for a complete 
list of supported environments:

   https://developer.theice.com/hc/en-us/articles/202681040-Compatibility-Matrix

Operating Systems:

   Windows 10
   Windows 8
   Windows 7
   Windows Vista
   WindowsServer2012
   WindowsServer2008
   WindowsServer2003
   Centos7.x - 64bit
   Centos6.x - 64bit
   RHEL 6.x - 64bit
   RHEL 7.x - 64bit
   Suse 11.X - 64 bit


3. DEPENDENCIES:
================

This release of the JStandard requires one of the following Java VMs:

   Oracle Java SE 8
   OpenJDK 8

Refer to the API the Compatibility Matrix for complete details.


4. PACKAGE STRUCTURE:
=====================

This JStandard API SDK contains the following directories and materials:

   README.TXT
      This file.
    
   \bin
      Contains the JStandard API's required dynamic link libraries (DLL) and
      shared object (SO) files for Windows and Linux platforms.

   \docs
      Contains the JStandard API's javadocs and third-party library licenses.

   \lib
      Contains the JStandard API's jars required for development.
 
   \samples
      Contains the source code for the JStandard API's console-based samples, 
      the libraries used by those samples, and .bat and .sh files that may be
      used to compile and run the samples.

   \third-party
      Contains third party libraries required by the JStandard API for development.

    
5. WHERE SHOULD I START?
========================

We recommend the "Getting Started" area in the online Developer Center.
The Developer Center provides instructions on how to unpack the SDK, an 
overview of how to configure a development environment, and a walk through 
of key API features and functions.

The online Developer Center is located at:

    https://developer.theice.com/hc/en-us

To log into the Developer Center click on the "Sign in" link at the top of 
the page and use the username and password that you use to access data
from the consolidated feed.

The following article discusses how to extract the SDK:

    https://developer.theice.com/hc/en-us/articles/200717124-Extracting-the-SDK


6. WHAT IF I NEED HELP?
=======================

If you need assistance and you are in a trial phase, please contact your Pre-Sales
Account Manager.  For all other inquiries please contact Desktop Client Support at:

    DesktopClientSupport@ice.com


7. MIGRATION:
=============

This section lists migration information per release. Only releases that have
migration information will be listed. If you are migrating from a previous
release, review any migration information between your previous release to your
target release. We also recommend reviewing the CHANGE LOG section as well.

01.021.0000:
    The SDK directory structure changed. Refer to the CHANGE LOG section
    for details on what changed.

01.020.0000:
    The Quote interface was updated to be more dynamic. Refer to the CHANGE LOG
    section for details. A migration guide can be found at:
    
    https://developer.theice.com/hc/en-us/articles/203644424-JStandard-1-20-0-Migration-Guide


8. ATTRIBUTION AND LICENSES FOR CERTAIN SOFTWARE COMPONENTS
===========================================================
A. Notices of Software Components Licensed Under the Apache License, Version 2.0

   -------------------------------------------------------------------------------------------------------------------
   Name                          File                               Copyright
   -------------------------------------------------------------------------------------------------------------------
   Apache Commons Collections    commons-collections4-4.4.jar       Copyright 2001-2019 The Apache Software Foundation
   Apache Commons Configuration  commons-configuration2-2.11.0.jar  Copyright 2001-2024 The Apache Software Foundation
   Apache Commons Lang           commons-lang3-3.17.0.jar           Copyright 2001-2024 The Apache Software Foundation
   Apache Commons BeanUtils      commons-beanutils-1.9.4.jar        Copyright 2000-2019 The Apache Software Foundation
   Apache Commons Text           commons-text-1.12.0.jar            Copyright 2014-2024 The Apache Software Foundation
   Commons Logging               commons-logging-1.3.4.jar          Copyright 2001-2024 The Apache Software Foundation
   Jackson-annotations           jackson-annotations-2.18.2.jar     Copyright 2002-2024 The Apache Software Foundation
   Jackson-core                  jackson-core-2.18.2.jar            Copyright 2002-2024 The Apache Software Foundation
   jackson-databind              jackson-databind-2.18.2.jar        Copyright 2002-2024 The Apache Software Foundation
   Apache Log4j2 API             log4j-api-2.24.2.jar               Copyright 1999-2024 The Apache Software Foundation
   Apache Log4j2 Core            log4j-core-2.24.2.jar              Copyright 1999-2024 The Apache Software Foundation
   Apache Log4j2 JCL             log4j-jcl-2.24.2.jar               Copyright 1999-2024 The Apache Software Foundation
   -------------------------------------------------------------------------------------------------------------------

   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
   except in compliance with the License. You may obtain a copy of the License at 
   https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software distributed under the 
   License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
   either express or implied. See the License for the specific language governing permissions 
   and limitations under the License.


B. Notices of Software Components Licensed Under the GNU Lesser General Public License v2.1 or later

   --------------------------------------------------------------
   Name                                 File                     
   --------------------------------------------------------------
   Java Native Access (JNA)             jna-3.5.2.jar         
   Java Native Access (JNA) Platform    platform-3.5.2.jar   
   --------------------------------------------------------------
   
   The above libraries are provided under the GNU Lesser General Public License, version 2.1 or later.
   Alternative license arrangements are negotiable.
   
   NOTE: Oracle is not sponsoring this project, even though the package name (com.sun.jna) might imply otherwise.
   
   The GNU Lesser General Public License (LGPL), version 2.1 may be found at https://www.gnu.org/licenses/lgpl-2.1.html 
   
   
C. Notices of Software Components Licensed Under the JSON.org Public Domain License 

   --------------------------------------------------------------
   Name                                 File                     
   --------------------------------------------------------------
   Java API for Processing JSON         json-20240303.jar      
   --------------------------------------------------------------
   
   The above libraries are provided under the JSON.org Public Domain License.
   
   The Public Domain License can be found at https://github.com/stleary/JSON-java/blob/master/LICENSE
   
D. Notices of Accompanying Licenses

   This SDK package includes the licenses described above.  The files are listed below:
   
   - Apache License - Version 2.0.txt
   - GNU Lesser GPL v2.1 or later.txt
   - Common Development and Distribution License 1.1.txt
   - GNU General Public License v2.0.txt  
   - JSON.org Public Domain License.txt
   
   
9. CHANGE LOG:
==============
01.031.0001:
   Enhancements/Fixes:
   ===================
   [JSTAN-992]   - Wrap DBCAPI with the fix for memory leak with DBCAPI_FLAG_EXPAND_DISPLAY_KEY.
   [MDAPI-148]   - Fix memory leak with DBCAPI_FLAG_EXPAND_DISPLAY_KEY.


   Details:
   ========
   [JSTAN-992]   - Wrap DBCAPI with the fix for memory leak with DBCAPI_FLAG_EXPAND_DISPLAY_KEY.
   [MDAPI-148]   - Fix memory leak with DBCAPI_FLAG_EXPAND_DISPLAY_KEY.

01.031.0000:
   Enhancements/Fixes:
   ===================
   [JSTAN-976]   - Support Factset snapshot requests.
   [JSTAN-986]   - Add QuoteRequest to the requestStockOptionRoots method call in QuoteManager.
   [MDAPI-142]   - Support Factset snapshot requests.


   Details:
   ========
   [JSTAN-976]   - Support Factset snapshot requests
   [JSTAN-986]   - Add QuoteRequest to the requestStockOptionRoots method call in QuoteManager.
   [MDAPI-142]   - Support Factset snapshot requests.
   
        With this release FactSet data and other Fundamental data will be accessible via the 
        QuoteManager interface.
        New enumeration DATASET_TYPE is added to com.esignal.jstandard.beans.quote.QuoteRequest:
        - FUNDAMENTAL
        - PRICING
   
        New Setter and Getter methods is added to com.esignal.jstandard.beans.quote.QuoteRequest:
        - void setDatasetType(DATASET_TYPE datasetType)
        - DATASET_TYPE getDatasetType()
   
        In order to get access FactSet data and other Fundamental data, API users should set the DatasetType 
        to DATASET_TYPE.FUNDAMENTAL.
      
        New interface methods in com.esignal.jstandard.managers.QuoteManager
        - void requestStockOptionRoots(String symbol, StockOptionRootsListener stockOptionRootsListener, 
        QuoteRequest quoteRequest);
		     This method is used to request stock option roots for a given symbol. We have added 
		     QuoteRequest to this method.  

01.030.0000
   Enhancements/Fixes:
   ===================
   [JSTAN-953]   - Add additional LRT Type constants to Quote.FieldItem
   [JSTAN-903]   - JStandard/dbcapi-jna supports SIP Fractional Share
   [JSTAN-942]   - JStandard/dbcapi-jna supports Nanosecond time
   [JSTAN-947]   - JStandard LightWeight supports SIP Fractional Share and Nanosecond time
   [JSTAN-919]   - JStandard SDK supports SIP Fractional Share and Nanosecond time
   [JSTAN-969]   - Update Fractional Shares rounding logic
   [JSTAN-961]   - Add new LRT Type constants to Quote.FieldItem and add new getFieldName interface
                   to Quote.FieldItem
   [JSTAN-968]   - Add Connection Flag DBCAPI_FLAG_EXPAND_DISPLAY_KEY for LRT_TYPE_DISPLAY_KEY in Quote Response
   [JSTAN-972]   - Unsubscribing Continuous Futures Instruments   

   Details:
   ========
   [JSTAN-953]   - Add additional LRT Type constants to Quote.FieldItem
   
        Quote.FieldItem constants have been updated to include missing LRTs.
   
   [JSTAN-903]   - JStandard/dbcapi-jna supports SIP Fractional Share
   [JSTAN-942]   - JStandard/dbcapi-jna supports Nanosecond time
   [JSTAN-947]   - JStandard LightWeight supports SIP Fractional Share and Nanosecond time
   [JSTAN-969]   - Update Fractional Shares rounding logic
        
        Fractional Shares support has been added to support fractional sizes and volumes.
        Nanosecond Time support has been added to support date/times with nanosecond precision.
        
        To get access to these features, API users should use the "Dynamic Discovery of new Quote fields"
        using the Quote.iterator() method described in Section 9 CHANGE LOG version 01.020.0000 of this
        README.txt as well as in the "Migration Guide for JStandard 01.020.0000.docx".
        
        As a reminder, QUOTEFIELDS are deprecated and will not support Fractional Shares and Nanosecond
        time. The following exception are:
            - In cases where nanosecond time is available upstream, millisecond time will be available
              in the QUOTEFILEDS (e.g., QUOTEFILEDS.DATETIMEBID_QUOTE_FIELD).
            - In cases where fractional size/volume is available upstream:
                - If the QUOTEFIELDS's type is a double (e.g., QUOTEFIELDS.TOTALVOL_QUOTE_FIELD) then
                  the value will contain fractional size/volume.
                - If the QUOTEFIELDS's type is a long (e.g., QUOTEFIELDS.LASTSIZE_QUOTE_FIELD) then the
                  the value will be rounded as follows:
                    - If the value is less than 1, round up to 1.
                    - If the value is greater than 1, round down to the nearest whole number.
        
        
        The following interfaces have changed:
        
            CidData, CidFieldData, CidPriceFieldData, LightWeightCidFieldData, LightWeightCidPriceFieldData:
                Deprecated: int getExchangeTime()
                Added: Instant getExchangeTimeAsInstant() for Nanosecond time support
                Added: long getExchangeTimeSeconds() for Nanosecond time support
                Added: long getExchangeTimeNanoseconds() for Nanosecond time support
                
                Deprecated: long getSequenceNumber()
                Added: BigInteger getSequenceNumberAsBigInteger() for large sequence number support
                
            TradeTickRecord, DefaultTradeTickRecord:
                Deprecated: long getVolume()
                Added: Double getVolumeAsDouble() for fractional shares support
                
                Deprecated: long getTotalVolume()
                Added: Double getTotalVolumeAsDouble()  for fractional shares support
                
            TradeTickRecordCID, DefaultTradeTickRecordCID:
                Deprecated: getVolumeQualifiers()
                Deprecated: setVolumeQualifiers()
            
            TradeTickRecordCIDInfo, DefaultTradeTickRecordCIDInfo:
                Deprecated: long getVolume()
                Added: Double getVolumeAsDouble() for fractional shares support
                
                Deprecated: long getPrevSequenceNumber()
                Added: BigInteger getPrevSequenceNumberAsBigInteger() for large sequence number support
                
            QuoteTickRecord, DefaultQuoteTickRecord 
                Deprecated: int getBidSize()
                Added: Double getBidSizeAsDouble() for fractional shares support
                Deprecated: int getAskSize()
                Added: Double getAskSizeAsDouble() for fractional shares support
            
            QuoteTickRecordCID, DefaultQuoteTickRecordCID
                Deprecated: getBidVolumeQualifiers()
                Deprecated: setBidVolumeQualifiers()
                Deprecated: getAskVolumeQualifiers()
                Deprecated: setAskVolumeQualifiers()
            
            HistoricalTickRecordCID, DefaultHistoricalTickRecordCID
                Deprecated: long getSequenceNumber()
                Added: BigInteger getSequenceNumberAsBigInteger()
        	
            HistoricalBar
                getVolume() has always returned Double values however it never returned fractional
                values. This method will now return fractional values, if supported upstream
                (i.e., by the exchange). 
        
        Libraries Upgraded:
            org.apache.commons:commons-configuration2 v2.10.1 to v2.11.0
            org.apache.commons:commons-lang3 v3.14.0 to 3.17.0
            commons-logging:commons-logging v1.2 to v1.3.4
            com.fasterxml.jackson.core:jackson-databind v2.14.2 to 2.18.2
            org.apache.logging.log4j:log4j-api v2.20.2 to v2.24.2
            org.apache.logging.log4j:log4j-core v2.20.2 to v2.24.2
            org.apache.logging.log4j:log4j-jcl v2.20.2 to v2.24.2
        
        
    [JSTAN-919]   - JStandard SDK supports SIP Fractional Share and Nanosecond time
       
        The samples packaged with the JStandard SDK 01-029-0000 include the following:
       
          - General changes to improve efficiency and readability
          - A change from using the javax.json-1.1.4.jar JSON library to the json-20240303.jar JSON library
          - A new SnapOnTriggerSample to demonstrate the new Snap-on-Trigger service
          - Changes to use ...getVolumeAsDouble() and ...getTotalVolumeAsDouble() to support fractional shares
          - Changes to use ...getExchangeTimeAsInstant() to support nanosecond time
          - Changes to use ...getSequenceNumberAsBigInteger() to support large sequence numbers
          - Changes to use ...getExchangeTimeSeconds() and ...getExchangeTimeNanoseconds() to support nanosecond time
          

    [JSTAN-961]   - Add new LRT Type constants to Quote.FieldItem and add new getFieldName interface
                  to Quote.FieldItem
                   
        Quote.FieldItem constants have been updated to include additional new LRT Types.
        Quote.FieldItem.getFieldName interface has been added to return the field name for a
        FieldItem or a fieldId.
        
    [JSTAN-968]   - Add Connection Flag DBCAPI_FLAG_EXPAND_DISPLAY_KEY for LRT_TYPE_DISPLAY_KEY in Quote Response
    
        ConnectionFlags.DBCAPI_FLAG_EXPAND_DISPLAY_KEY has been added. When subscribing to a
        Continuous Futures Instrument, you need to call QuoteManager.connect() with ConnectionSettings
        flags as ConnectionFlags.CONNECTION_FLAGS.DBCAPI_FLAG_EXPAND_DISPLAY_KEY. This will ensure that
        LRT_TYPE_DISPLAY_KEY will be returned in the Quote Responses and will contain the continuous
        futures instrument. Without this flag, the QuoteListener will not receive the responses for
        continuous futures instruments.
        
    [JSTAN-972]   - Unsubscribing Continuous Futures Instruments
    
        Unsubscribing of Continuous Futures Instruments was not working correctly when the original
        continuous futures instrument was specified. See the note regarding
        ConnectionFlags.CONNECTION_FLAGS.DBCAPI_FLAG_EXPAND_DISPLAY_KEY in JSTAN-968 above.
     

01.028.0000
   Enhancements/Fixes:
   ===================
   [JSTAN-920]   - Add Support for Snap-on-Trigger Service
   [JSTAN-922]   - Update SDK Samples and Add Snap-on-Trigger
   [MDAPI-127]   - Add Support in DBCAPI for Snap-on-Trigger Service
   

   Details:
   ========
   [JSTAN-920]   - Add Support for Snap-on-Trigger Service
   [MDAPI-127]   - Add Support in DBCAPI for Snap-on-Trigger Service
   
       This release includes a new service offering, Snap-on-Trigger.
       The Snap-on-Trigger service allows clients to receive a snapshot of data for one or more
       instruments each time there is a change to a specified field.  For example, clients
       may instruct the Snap-on-Trigger service to provide a snapshot each time the
       52 Week High updates for a set of instruments.  
       
       The Snap-on-Trigger service is similar to the existing Level 1 Data snapshot service
       available from the QuoteManager, but it is a separate and distinct server type within
       the ICE Data Services network.
       
       Here are some key differences between the existing Level 1 Data snapshot service and the
       new Snap-on-Trigger service:
       
       ---------------------------------------------------------------------------------------------------------------------------------
       FEATURE                                   |  LEVEL 1 DATA SNAPSHOT SERVICE  |   SNAP-ON-TRIGGER SERVICE   
       ------------------------------------------+---------------------------------+----------------------------------------------------
       Number of instruments per request         |  1 instrument only              |   Multiple instruments per trigger
       ------------------------------------------+---------------------------------+----------------------------------------------------       
       Number of snapshots returned per request  |  1 snapshot with most current   |   The Snap-on-Trigger server watches for changes 
                                                 |  data available for the symbol. |   to any of the "trigger fields" for each symbol
                                                 |                                 |   associated with a trigger.  The service provides 
                                                 |                                 |   a current snapshot each time one of the fields
                                                 |                                 |   changes for one or more symbols in the group.
                                                 |                                 |   Snapshots are sent for those symbols that have
                                                 |                                 |   updated fields only. 
       ------------------------------------------+---------------------------------+----------------------------------------------------
       Ports used to connect to the service      | 2189 - non-secured TCP/IP       |   2191 - non-secured TCP/IP
                                                 | 3189 - secured TLS              |   * secured TLS not currently supported
       ------------------------------------------+---------------------------------+----------------------------------------------------

       Clients create "triggers" as JSON-formatted Strings:
       
          {"triggerName":<triggerName>,"triggerID":<triggerID>,"triggerType":"field","symbols":[<"symbol">,<"symbol">,<"symbol">,...],"fields":[<fieldID>,<fieldID>,<fieldID>,...]}
          
          where:
          
             <triggerName>                    is a unique (case-sensitive) name for the trigger.
             
             <triggerID>                      is a unique ID to help distinguish one trigger from another.
                                              The <triggerName> and <triggerID> must be specific to each defined
                                              trigger.
             
             <"symbol">                       One or more instruments surrounded by double-quotes and separated by
                                              commas.
                                              
             <fieldID>                        The numeric ID associated with a Quote.FieldItem.  A list of common
                                              fields and their associated IDs may be found on the Developer Center:
                                              
                                                 https://developer.theice.com/hc/en-us/articles/205935720-Level-1-Data-Dictionary
                                                 
             note:  Currently, "triggerType" supports "field" only.
          
          Example:
          
             {"triggerName":"Ask Price","triggerID":1,"triggerType":"field","symbols":["IBM","ICE","GOOG","TSLA","VOD-LON"],"fields":[21]}
             
       
       Refer to the samples packaged with this SDK and the Developer Center for more details regarding
       the Snap-on-Trigger service and related requests.
         
   [JSTAN-922]   - Update SDK Samples and Add Snap-on-Trigger
   
       The samples packaged with the JStandard SDK 01-028-0000 include the following:
       
          - General changes to improve efficiency and readability
          - A change from using the javax.json-1.1.4.jar JSON library to the json-20240303.jar JSON library
          - A new SnapOnTriggerSample to demonstrate the new Snap-on-Trigger service
   
       This JStandard SDK includes the JStandard API version 01-028-0002.
       
       NOTE: As a result of the JSON library change, the java.json-1.1.4.jar is no longer a
             part of the package.
   
01.027.0001:
   Enhancements/Fixes:
   ===================
   [JSTAN-912]   - Package JStandard SDK 1.27.0.1 with DBCAPI 1.27.0.274 (Fix socket close issue)
   [MDAPI-130]   - TLS implementation does not close sockets properly


   Details:
   ========
   [JSTAN-912]   - Package JStandard SDK 1.27.0.1 with DBCAPI 1.27.0.274 (Fix socket close issue)
   [MDAPI-130]   - TLS implementation does not close sockets properly
   
       This release addresses an issue with how the API closed TLS sockets.
       This release closes TLS sockets cleanly and swiftly.       


01.027.0000:

   Enhancements/Fixes:
   ===================
   [JSTAN-865]   - Update JStandard Library with New DBCAPI to Support TLS connections

   [JSTAN-875]   - Expose methods for users to tell the API to use a secure (TLS) connection (Windows Only).
   
   [JSTAN-857]   - Update Third-Party Apache Libraries.
   
   [JSTAN-890]   - Delete International Tick History Manager functionality from JStandard.
   
   [MDAPI-126]   - Support Secure Transport for JStandard/Standard API Client Connections (Windows Only).
   
   Details:
   ========
   [JSTAN-865]   - Update JStandard Library with New DBCAPI to Support TLS connections
   
       The JStandard library wraps the features and functions of the DbcAPI library.
       This release includes the latest release of the DbcAPI library.
       Upgraded and added the following dll files:
       -----------------------------------------------------------------
       Old Library                        New Library
       -----------------------------------------------------------------
       dbcapi_64VC14.dll                  dbcapi_64VC17.dll
       PortLib_64VC14.dll                 PortLib_64VC17.dll
                                        * libssl-3-x64.dll          
                                        * libcrypto-3-x64.dll
       -----------------------------------------------------------------
       *  libssl-3-x64.dll and libcrypto-3-x64.dll were added to support TLS
          connections.  These are new dependencies required to run the 
          dbcapi_64VC17.dll.
       
   [JSTAN-875]   - Expose methods for users to tell the API to use a secure (TLS) connection (Windows Only).
   [MDAPI-126]   - Support Secure Transport for JStandard/Standard API Client Connections (Windows Only).
    
        This feature provides a way to use a secure (i.e., encrypted) connection to ensure account
        credentials (username/password) are protected between clients and the ICE Data Services network.
        This release supports TLS-encrypted TCP connections to the ICE Data Services network for the
        Windows platform only; this feature is not yet supported on Linux platforms.
        
        NOTE:  Secure (TLS) connections require the following ports are unblocked for use:
        
                  3189 - Financial Quotes Server      (QuoteManager)
                  3190 - News Server                  (NewsManager)
                  3192 - Intraday History Server      (TickHistoryManager)
                  3194 - Interday History Server      (HistoryManager)
                  3196 - Market Depth Server          (MarketDepthManager)

               In addition to the above port requirements, client accounts must be entitled to
               receive DNS server name addresses for routing to servers that support TLS connections:
               
                  NATR     CM routing by server name     1023                

   [JSTAN-857]   - Update Third-Party Apache Libraries.
   
       The following list identifies the open source libraries that were updated:
       --------------------------------------------------------------------
       Old Library                        New Library
       --------------------------------------------------------------------
       commons-collections-3.2.2.jar      commons-collections4-4.4.jar
       commons-configuration-1.10.jar     commons-configuration2-2.10.1.jar
       commons-lang-2.6.jar               commons-lang3:3.14.0          
                                        * commons-beanutils-1.9.4.jar
                                        * commons-text-1.11.0.jar
       --------------------------------------------------------------------
       *  commons-beanutils-1.9.4.jar and commons-text-1.11.0.jar are 
          dependencies for commons-configuration2-2.10.1.jar.

   [JSTAN-890]   - Delete International Tick History Manager functionality from JStandard.
   
       Property 'jstandard.DBC.InternationalTickHistory.enable' has been removed and 
       can no longer be used.  TickHistoryManager supports both the domestic and 
       international symbols.
       

01.025.0003:

   Enhancements/Fixes:
   ===================
   [JSTAN-854]   - Update SDK to Include Latest Log4j and Jackson Libraries
   [JSTAN-835]   - Update SDK to Fix Vulnerabilities in JStandard API
   [JSTAN-837]   - Update SDK to Fix XML Injection Vulnerability in JStandard API
   
   Details:
   ========
   [JSTAN-854]   - Update SDK to Include Latest Log4j and Jackson Libraries
   
       NOTE:  This release of the JStandard SDK addresses Log4j and Jackson vulnerabilities only. 
       
       The samples packaged with the JStandard SDK use the Apache Log4j libraries
       to assist in various logging levels.  The National Institute of Standard and 
       Technology has identified previous version of the Log4j libraries as vulnerable
       to a DOS exploit.  This update addresses the following vulnerabilities from
       the National Vulnerability Database:
       
          CVE-2021-45105
          
       JStandard API uses com.fasterxml.jackson.core libraries for Flex data.
       The National Institute of Standard and Technology has identified previous 
       versions of the com.fasterxml.jackson.core jackson-databind library as
       vulnerable.  This update addresses the following vulnerabilities from the
       National Vulnerability Database:
       
          CVE-2022-42004
          CVE-2022-42003
          CVE-2021-46877
          CVE-2020-36518
          
       This update includes the following version of the Log4j and Jackson libraries:
       
          log4j-api-2.20.0.jar
          log4j-core-2.20.0.jar
          log4j-jcl-2.20.0.jar
          jackson-annotations-2.14.2.jar
          jackson-core-2.14.2.jar
          jackson-databind-2.14.2.jar
          
   [JSTAN-835]   - Update SDK to Fix Vulnerabilities in JStandard API
   
       The Common Weakness Enumeration (a.k.a, CWE) has identified the following
       vulnerabilities:
       
          CWE-611
          CWE-117
   
   [JSTAN-837]   - Update SDK to Fix XML Injection Vulnerability in JStandard API
   
       The Common Weakness Enumeration (a.k.a, CWE) has identified the following
       vulnerabilities:
       
          CWE-611
   
   NOTE: JStandard API version 1.25.3.0 contains no changes from the previous
             release.

01.025.0002:

   Enhancements/Fixes:
   ===================
   [JSTAN-834]   - Update SDK to Include Latest Log4j Libraries
      
   Details:
   ========
   [JSTAN-834]   - Update SDK to Include Latest Log4j Libraries
   
       NOTE:  This release of the JStandard SDK addresses Log4j vulnerabilities only. 
       
       The samples packaged with the JStandard SDK use the Apache Log4j libraries
       to assist in various logging levels.  The National Institute of Standard and 
       Technology has identified previous version of the Log4j libraries as vulnerable
       to an RCE exploit.  This update addresses the following vulnerabilities from
       the National Vulnerability Database:
       
          CVE-2021-44228
          CVE-2021-45046
          
       This update include the following version of the Log4j libraries:
       
          log4j-api-2.16.0.jar
          log4j-core-2.16.0.jar
          log4j-jcl-2.16.0.jar
          
       NOTE: JStandard API version 1.25.2.0 contains no changes from the previous
             release.

01.025.0001:

   Enhancements/Fixes:
   ===================
   [JSTAN-825]   - Update JStandard Library with DbcAPI Version 01.025.0001

   [JSTAN-824]   - Fix Incorrect Credentials Returned by acquireUsername
   
   [MDAPI-106]   - Add Pipe ( | ) Delimited Host List Support for AcquireUsername
   
   [MDAPI-104]   - Fix AcquireUsername Exit Issue for Accounts without Required Entitlements
   
   [MDAPI-103]   - Fix Crash on Linux Platforms When Logging to File from Multiple Threads
   
   Details:
   ========
   [JSTAN-825]   - Update JStandard Library with DbcAPI Version 01.025.0001
   
       The JStandard library wraps the features and functions of the DbcAPI library.
       This release includes the latest release of the DbcAPI library.

   [JSTAN-824]   - Fix Incorrect Credentials Returned by acquireUsername
   
       The acquireUsername interface incorrectly returned the username for the password.
       This issue has been fixed with this release.

   [MDAPI-106]   - Add Pipe ( | ) Delimited Host List Support for AcquireUsername
       The previous release did not support pipe ( | ) delimited host names submitted to
       the AcquireUsername interface.  This release now supports pipe ( | ) delimited
       host names sent to the AcquireUsername interface.

   [MDAPI-104]   - Fix AcquireUsername Exit Issue for Accounts without Required Entitlements
   
       The AcquireUsername interface did not immediately exit if the host returned
       DBCAPI_ERROR_NOT_ENTITLED.  This release corrects that issue and immediately exits
       if an account that is not entitled to use the AcquireUsername interface attempts
       to use it.

   [MDAPI-103]   - Fix Crash When Logging to File from Multiple Threads
   
       Previous releases used ctime() when writing to log files. This resulted in
       a crash if multiple threads were trying to write to the log file.
       To correct this potential issue, this release uses ctime_r() for Linux platforms, 
       and ctime_s() for Windows platforms.

01.025.0000:

   Enhancements/Fixes:
   ===================
   [JSTAN-801]   - Support new FLEX data types CHAR, INT8 and UINT8 for Daily  
                   History Server and Intraday History Server FLEX data requests.
                   
   [JSTAN-785]   - Report JStandard library name changes.
                   
   [JSTAN-770]   - JStandard API no longer supports Java JDK 1.7.
   
   [JSTAN-759]   - Upgrade open source libraries used by the JStandard API to
                   new versions that correct CVEs.
                   
   [JSTAN-758]   - Introduce acquireUsername function to allow clients the ability 
                   to fetch a new credential from their pool of credentials.
                   
   [MDAPI-100]   - DbcAPI now uses C++11 to replace third-party MD5 libraries.
    
   [MDAPI-98]    - Fix crash on DbcGetServerIPFromCM and DbcGetServerIPFromCMEx
                   when the nBufSize is set to 0.
                   
   [MDAPI-96]    - Fix issue with DbcCloseConnection hash table to use 64-bit 
                   key on the 64-bit platform to prevent collisions on 64-bit 
                   connection pointers.
                   
   [MDAPI-93]    - Fix program_name on DbcInitializeEx to handle flexdata calls.
   
   [MDAPI-91]    - Modify DbcDictionaryRequest to send appid and appversion on 
                   request.
                   
   Details:
   ========
   [JSTAN-801]   - Support new FLEX data types CHAR, INT8 and UINT8 for Daily  
                   History Server and Intraday History Server FLEX data requests.

       note:  Flexible data requests are available to desktop offerings only.
       
       Support new FLEX data types: CHAR, INT8 and UINT8 for Daily History Server 
       and Intraday History Server FLEX data requests.
       
       The Developer Center (https://developer.theice.com/hc/en-us) has
       articles that explain how the ICE Data Services Desktop offerings can use
       and access Flex data:

           - History: Intraday Tick and Bar History 
           - History: Interday Bar History

   [JSTAN-785]   - Report JStandard library name changes.

       The following JStandard library file names have changed:
       
       -----------------------------------------------------------------------
       Old Library Name                   New Library Name
       -----------------------------------------------------------------------
       esignal-jstandard.jar             ICEesig-jstandard-api-1.25.0.1.jar
       esignal-jstandard-dbcjna.jar      ICEesig-jstandard-dbcjna-1.25.0.1.jar
       -----------------------------------------------------------------------
       
       note: The new file name structure includes the version numbers.
             The versions listed above were current at the time of this writing,
             but may have changed prior to release.                   

   [JSTAN-770]   - JStandard API no longer supports Java JDK 1.7.
   
       Support for Java JDK 1.7 has been removed.  The JStandard API
       now requires Java 8.

   [JSTAN-759]   - Upgrade open source libraries used by the JStandard API to
                   new versions that correct CVEs.

       The following list identifies the open source libraries that were updated:
       
       -----------------------------------------------------------------
       Old Library                        New Library
       -----------------------------------------------------------------
       commons-collections-3.2.1.jar      commons-collections-3.2.2.jar 
       commons-configuration-1.5.jar      commons-configuration-1.10.jar
       commons-lang-2.4.jar               commons-lang-2.6.jar          
       commons-logging-1.1.1.jar          commons-logging-1.2.jar       
       jackson-annotations-2.1.5.jar      jackson-annotations-2.11.2.jar
       jackson-core-2.1.5.jar             jackson-core-2.11.2.jar       
       jackson-databind-2.1.5.jar         jackson-databind-2.11.2.jar   
       log4j-1.2.13.jar                   log4j-api-2.13.3.jar          
                                          log4j-core-2.13.3.jar         
                                          log4j-jcl-2.13.3.jar          
       javax.json-1.0.jar                 javax.json-1.1.4.jar          
       -----------------------------------------------------------------

       note: The log4j library has grown from 1 jar file to 3 jar files.

   [JSTAN-758]  - Introduce acquireUsername function to allow clients the ability 
                  to fetch a new credential from their pool of credentials.

       The Amazon Web Services (AWS) environment dynamically scales available 
       capacity up and down, therefore the origin address of the AWS server from 
       which a client connects is not static. Exchange requirements concerning 
       "single device" usage indicate that clients shall not connect from multiple 
       origins simultaneously. For each connection session, there must be one
       and only one origin address reported for a given client username. For most
       client implementations, the dynamic architecture of AWS poses no issue as 
       the origin address remains stable for the duration of the client connection,
       and the ICE Data Services network does not permit a client to connect from
       multiple origins simultaneously. 
       
       Some client implementations have multiple machines where each machine 
       services a specific pool of usernames. Since the Amazon Web Services 
       environment is dynamic, neither the implementing client nor ICE Data 
       Services can rely upon a static origin address to associate a specific 
       pool of usernames to a specific client machine.  The acquireUsername 
       interface provides a way to associate a specific pool of usernames to a
       specific client machine in a dynamic environment.
       
       The acquireUsername is a new asynchronous method that allows a client to
       pass "Master Account" credentials to the Connection Manager. Each "Master
       Account" has an associated pool of usernames. Upon request, the Connection
       Manager verifies the "Master Account" credentials, determines which 
       username pool to use and returns the details for an available username
       from that pool. This allows the client machine to move within the AWS
       cloud, while still servicing a specific pool of usernames and maintain
       the integrity of "single device" usage for each of those usernames.

   [MDAPI-100]   - DbcAPI now uses C++11 to replace third-party MD5 libraries.

       The DbcAPI now uses C++11 to perform MD5 hashing. As a result, the following
       third-party libraries are no longer needed and will not be included in the
       SDK:
       
       - libmd564.so.0
       - MD5Lib_64VC14.dll

   [MDAPI-98]   - Fix crash on DbcGetServerIPFromCM and DbcGetServerIPFromCMEx
                  when the nBufSize is set to 0.

       Fixed an issue where the DbcGetServerIPFromCM was crashing when nBufSize 
       is set to 0.

   [MDAPI-96]    - Fix issue with DbcCloseConnection hash table to use 64-bit 
                   key on the 64-bit platform to prevent collisions on 64-bit 
                   connection pointers.
       
       Fixed a problem with the DbcCloseConnection on a 64-bit platform when 
       there is a large number of connections opened. There was a issue with 
       DbcCloseConnection returning the DBCAPI_ERROR_INVALIDSID code for a 
       valid connection.

   [MDAPI-93]    - Fix program_name on DbcInitializeEx to handle flexdata calls.
       
       DbcInitializeEx was fixed to set the program_name which is the same 
       behavior as DbcInitializeEx2.
       
   [MDAPI-91]    - Modify DbcDictionaryRequest to send appid and appversion on 
                   request.

       With this fix there is a change in the JSON response for the 
       DbcDictionaryRequest call. The old version returned the header with an 
       aapid and appversion that were the same. With the fix, the response 
       string will have the appid as the "jstandard.DBC.program" and the 
       appversion is the version and other information about the C Standard 
       library used.
       
       New response example: 
       "header": {
           "queryname": "mydictionary",
           "requestid": 1234,
           "querytype": "dictionary",
           "cobrandid": 0,
           "appid": "<jstandard.DBC.program>",
           "appversion": "247 10/20/20 JSTA",
           "turnaround": 2,
           "userid": "<USERID>",
           "version": 17
       }
   
01.024.0007:

    Enhancements/Fixes:
    ===================
    [JSTAN-757]   - LW API needs to communicate over HTTPS.
    [JSTAN-760]   - LW API needs to communicate with POST instead of GET.

    Details:
    ========
    [JSTAN-757] - The LightWeight API now communicates over HTTPS to the 
                  ICE Data Services network. If Java 7 is used, TLS 1.2 will be
                  used.
    [JSTAN-760] - The LightWeight API now uses the POST method to transmit data 
                  to the ICE Data Services network.
01.024.0006:

    Enhancements/Fixes:
    ===================
    [JSTAN-699]   - Add support for OpenJDK 8 and version alignment with C API.
    [JSTAN-671]   - Add LRT Type constants to Quote.FieldItem and update Quote
                    samples.
    [JSTAN-384]   - Change default ByteOrder to Little Endian and fix
                    readAsByteBuffer to set the byte order.
    [JSTAN-664]   - Fix memory leak in ResourceManagers due to shutdown hooks.
    [JSTAN-682]   - Fix minor JStandard SDK bug in SymbolCheckSample.getData
    [JSTAN-725]   - Expose the local connection IP address from JStandard.
    [MDAPI-88]    - Fix size calculation in DbcGetFlexDataSchema.
    [MDAPI-89]    - Fix crc32 crash under OpenJDK.
    [CNTSRVR-195] - Expand the limit on search filter and category filter from
                    4000 chars to 30000 chars.
    
    Details:
    ========
    [JSTAN-699] - Add support for OpenJDK 8 and version alignment with C API.

        New version of DbcAPI to support OpenJDK 8. See MDAPI-89.

    [JSTAN-671] - Add LRT Type constants to Quote.FieldItem and update Quote
                  samples.

        LRT Type constants have been added to Quote.FieldItem for reference.
        The Quote samples have been updated to use the Quote.FieldItem constants.

    [JSTAN-384] - Change default ByteOrder to Little Endian and fix
                  readAsByteBuffer to set the byte order.

        The default byte order (e.g. Endianness) between DbcAPI and JStandard
        was intended to be Little Endian, but was being set as Native. If running
        on non Intel X86 and non AMD64 processors, this could cause issues.
        This fix sets the default byte order to Little Endian.

    [JSTAN-664] - Fix memory leak in ResourceManagers due to shutdown hooks.

        ResourceManagers (for example, QuoteManager) register a shutdown hook
        when connect is invoked. When ResourceManager's disconnect is invoked,
        the shutdown hook was not being unregistered, causing a memory leak.
        This has been corrected.

    [JSTAN-682] - Fix minor JStandard SDK bug in SymbolCheckSample.getData

        A variable in SymbolCheckSample.getData() was being incremented within
        the if block, but needed to be before the if block. This has been
        corrected.
    
    [JSTAN-725] - Expose the local connection IP address from JStandard.
    
        From the ConnectionEvent object which is received from the 
        ConnectionListner.onConnected() callback, ConnectionEvent.getConnectionLocalIp()
        will return the local connection IP address of the connection.
        
    [MDAPI-88]  - Fix size calculation in DbcGetFlexDataSchema.

        Increases the schema name length from 32 to 128 bytes, in DbcAPI.

    [MDAPI-89]  - Fix DbcAPI crc32 crash under OpenJDK.

        Fixes a conflict with libc crc32 and crc32 of MDAPI which was causing
        DbcAPI to crash under OpenJDK 8.

    [CNTSRVR-195] - Expand the limit on search filter and category filter from
                    4000 chars to 30000 chars.

        Increases the limit on search filter and category filter from 4000
        chars to 30000 chars for the NewsManager requests.

01.024.0000:

    Enhancements/Fixes:
    ===================
    [JSTAN-489] - Add support for the new Flex protocol for Tick Server and
                  History Server required for the Desktop offerings.
    [JSTAN-491] - Populate values for the HistoricalTickRecord getFlags() 
                  method returned from the TickHistoryManager.
    [JSTAN-572] - Deprecate the setFlags method that is exposed via BarEvent.
    [JSTAN-607] - Linux MD5 library name changed to remove underscore for
                  consistency with other Linux libraries.
    [MDAPI-51, MDAPI-54] Removed 32 bit support.
    [JSTAN-529] - Removed support for Java SE 5 and Java SE 6.
    [MDAPI-25] - Make TCP window symmetrical for sending and receiving
    [MDAPI-1] - Fixed thread handle leak on the connection thread for windows
    [MDAPI-75] - Fixed no data value issue for SubscribeDepthSymbol request on 
                 Linux for MarketDepthManager. 
    
    Details:
    ========
    [JSTAN-489] - Add support for the new Flex protocol for Tick Server and
                  History Server required for the Desktop offerings.

        The Developer Center (https://developer.theice.com/hc/en-us) has
        articles that explain how the Desktop offerings can use the Flex 
        functionality to access Flex data. See:

            - History: Intraday Tick and Bar History 
            - History: Interday Bar History
    
    [JSTAN-491] - Populate values for the HistoricalTickRecord getFlags()
                  method returned from the TickHistoryManager.

        We will now be populating valid values for the getFlags() call inside 
        the HistoricalTickRecord.

    [JSTAN-572] - Deprecate the setFlags method that is exposed via BarEvent.

        We erroneously exposed the setFlags() function in the BarEvent. This
        has been deprecated.

    [JSTAN-607] - Linux MD5 library name changed to remove underscore for
                  consistency with other Linux libraries.

    [MDAPI-51, MDAPI-54] Removed 32 bit support.

        We will no longer support 32 bit version of windows.

    [JSTAN-529] - Removed support for Java SE 5 and Java SE 6.

        We will no longer support Java SE 5 and Java SE 6.

    [MDAPI-25] - Make TCP window symmetrical for sending and receiving

        Set the sender buffer size to match the receiver buffer size so the 
        socket can buffer data for heavy request clients on the socket. By 
        default, the API uses 256K for both the sender/receiver buffers.
        
    [MDAPI-1] - Fixed thread handle leak on the connection thread for windows
    
          Fixed the issue when thread handle leak occurred for each failed 
          connection attempt.
          
    [MDAPI-75] - Fixed no data value issue for SubscribeDepthSymbol request on 
          Linux for MarketDepthManager.


01.023.0000:

    Enhancements/Fixes:
    ===================
    [JSTAN-417] - Add two new values to the HistoricalBar.AGGREGATIONS
                  enumeration. 
    [JSTAN-418] - NoSuchElementException could be thrown when processing Quotes
                  from the QuoteManager for some exchanges.
    [JSTAN-421] - Add getAdjustment and getTradeDate methods to HistoricalBar
                  in order to expose adjustment and trade date.

    Details:
    ========

    [JSTAN-417] - Add two new values to the HistoricalBar.AGGREGATIONS
                  enumeration.

        The helper enumeration HistoricalBar.AGGREGATIONS has been updated to
        support SIMPLE_YIELD and THIRTY_DAY_YIELD for Funds that support it.

    [JSTAN-418] - NoSuchElementException could be thrown when processing Quotes
                  from the QuoteManager for some exchanges.

        A NoSuchElementException could be thrown when processing Quotes from
        the QuoteManager for some exchanges (for example: LON and LSIN). This
        bug was introduced in JStandard 01.020.0000.

        Below are the Quote methods that could have incorrectly thrown a
        NoSuchElementException:

        - Quote.getFieldItem(short fieldId)
        - Quote.isFieldItemNull(short fieldId)
        - Iterator<Quote.FieldItem>.next() from Quote.iterator() call. 

        Below are the deprecated QUOTEFIELDS Quote methods that could have
        incorrectly thrown a NoSuchElementException. Once a deprecated 
        QUOTEFIELDS Quote method is used, additional quotes received by the
        QuoteManager would be discarded and never returned to the user, due to
        an internal exception incorrectly occurring during processing of the
        quote received:

        - Quote.getFields()
        - Quote.getField(QUOTEFIELDS)
        - Quote.isFieldNull(QUOTEFIELDS)
        - Quote.getRawField(QUOTEFILEDS)
        - Quote.getFieldType(QUOTEFIELDS)

    [JSTAN-421] - Add getAdjustment and getTradeDate methods to HistoricalBar
                  in order to expose adjustment and trade date.

        The new HistoricalBar.getTradeDate() and HistoricalBar.getAdjustment()
        methods can be used to get the trade date and adjustment values from
        each HistoricalBar.

01.022.0000:

    Enhancements/Fixes:
    ===================
    [JSTAN-342] - Implement Floating NAV changes for HistoryManager.
    [JSTAN-387] - Fix MarketDepthManager to allow negative turnaround values.

    Details:
    ========

    [JSTAN-342] - Implement Floating NAV changes for HistoryManager.

        SEC Money Market reform changes to implement Floating NAV changes in
        the HistoryManager.
        
        HistoricalBarEvent.getFlags() method has been added to get a bitmap of
        flags from the History Server.
        
        A new helper enumeration HistoricalBar.AGGREGATIONS has been added:

        - getKey() for use when creating DbcKeyValuePairRequestOption objects
          for requests. 

        - getValue() for use when creating DbcKeyValuePairRequestOption objects
          for requests.

        - getBitMask() for use when receiving HistoricalBarEvents. Check the mask
          against HistoricalBarEvents.getFlags() to determine if the specific
          Aggregation type is present. 

    [JSTAN-387] - Fix MarketDepthManager to allow negative turnaround values.

        - In previous releases, the MarketDepthManager would not allow negative
          turnaround values. This restriction has been removed. The MarketDepthManager
          now allows negative turnaround values.

01.021.0000:

    Enhancements/Fixes:
    ===================
    [JSTAN-49]  - Restructure the JStandard SDK.
    [JSTAN-306] - Ability to specify a global connection timeout.
    [JSTAN-307] - Support for 64-bit turnaround ID for Tick and History Server
                  requests.
    [JSTAN-321] - Cleanup usage of DbcAPI History Bar functions.
    [TTP-39946] - Fix Incorrect categorization of specific ETF Mutual Funds.
    [TTP-40345] - Fix crash on Historical Tick requests.

    Details:
    ========

    [JSTAN-49] - Restructure the JStandard SDK.

        - A new README.TXT (this file) at the base directory replaces build.txt
          and ReadMeFirst.txt.

        - The third party libraries that used to reside in the lib directory are
          now located in the third-party\lib directory.

        - The dist directory has been removed. The JStandard API's jars are now
          located in the lib directory.

        - The JStandard Javadocs are now included under the docs\javadoc
          directory.

        - The samples source code, which previously resided in the src directory,
          now resides in the samples\src directory.

        - A new samples\bin directory contains helper scripts to compile and run
          the examples.

        - A new samples\lib directory contains libraries specific to the samples.

    [JSTAN-306] - Ability to specify a global connection timeout. 

        Previously the connection timeout setting was not exposed to the clients.
        The DBCResource manager now supports a global connection timeout which
        can be specified in two ways.

        - The jstandard.properties jstandard.DBC.timeouts.connection
          property can be used to set the global connection timeout
          (in milliseconds).

        - The ConnectionSettings class now exposes two methods to get or set the
          global connection timeout (in milliseconds), getConnectionTimeout()
          and setConnectionTimeout(int). Specify the connection timeout in the
          ConnectionSettings object and pass it to the resource manager's
          connect() method.  

        A connection timeout specified in the ConnectionSettings will take
        precedence over the jstandard.DBC.timeouts.connection in the
        jstandard.properties. If neither are specified, the default connection
        timeout is 30 seconds. The minimum value allowed is 1 second.

        Note that passing a ConnectionSettings object with a connectionTimeout
        to a  resource manager's connect() method will set the global connection 
        timeout and will affect new connections as well as any existing
        connection that experiences a reconnect.

    [JSTAN-307] - Support for 64-bit turnaround ID for Tick and History Server
                  requests.

        Previously, turnaround values over 32bits using the HistoryManager and
        TickHistoryManager would throw an exception on Windows systems on the
        request; and no response would be received on Linux. 64bit turnaround
        values are now supported. This change should not affect any existing
        client code.

    [JSTAN-321] - Cleanup usage of DbcAPI History Bar functions.

        Clean up our internal usage of DbcAPI History Bar functions. This
        change does not affect existing client code.

    [TTP-39946] - Fix Incorrect categorization of specific ETF Mutual Funds.

        In previous releases, Quote responses, where the symbol is a seven
        character ETF Mutual fund ending in NX, would contain a 
        Quote.FieldItem with id of 1 (e.g. Category) whose value was incorrectly
        set to 66 ('B' -> Stock). It will now be set correctly as 75
        ('K' -> Mutual Fund). For the deprecated QUOTEFIELDS,
        CATEGORY_QUOTE_FIELD's value was being incorrectly set to 
        CATEGORY.STOCK. It will now be set correctly as CATEGORY_MUTUALFUND.

    [TTP-40345] - Fix crash on Historical Tick requests.

        In previous releases, Historical Tick requests with symbols over 32
        bytes in length that contain dashes could cause JStandard to crash.
        This has been corrected.

01.020.0004:

    Fixes:
    ======
    [JSTAN-287] - Pick up DbcAPI version that fixes an issue where a crash
                  could occur while establishing a connection if a hostname
                  is not resolved within sixty seconds.

    Details:
    ========

    [JSTAN-287] - Pick up DbcAPI version that fixes an issue where a crash
                  could occur while establishing a connection if a hostname
                  is not resolved within sixty seconds.

        Resolved an issue in the native DbcAPI layer where a crash could occur
        while establishing a connection if a hostname is not resolved within
        sixty seconds.

01.020.0003:

    Fixes:
    ======
    [JSTAN-278] - Individual subscriptions and chain subscription to stock
                  options are not receiving updates.

    Details:
    ========

    [JSTAN-278] - Individual subscriptions and chain subscription to stock
                  options are not receiving updates.

        Resolved an issue where individual subscriptions and chain
        subscriptions to stock options received initial snapshot, but did not
        receive updates.

01.020.0002:

    Fixes:
    ======
    [JSTAN-267] - DbcNewsManager responses for snapshot requests return
                  incorrect data for some of the story id's. 

    Details:
    ========

    [JSTAN-267] - DbcNewsManager responses for snapshot requests return
                  incorrect data for some of the story id's. 

        Responses received from making news snapshot requests were
        returned with incorrect data where the storyId was not matching the
        proper headline text. This issue has been fixed in this release.

01.020.0001:

    Fixes:
    ======
    [JSTAN-68]  - QuoteManager doesn't properly manage listeners for
                  StockOptionRoots and StockOptionMonths
    [JSTAN-234] - Fix for back-to-back multiple OSI21 and IDCO22 LOOKUPTYPE
                  requests.
    [JSTAN-235] - Chunked responses from the Tick server results in inaccurate
                  data sets.

    Details:
    ========
                  
    [JSTAN-68]  - QuoteManager doesn't properly manage listeners for
                  StockOptionRoots and StockOptionMonths

        Multiple listeners calling requestStockOptionRoots or
        requestOptionMonth methods for the same symbol before a response is
        received, results in only the last listener getting a response.

    [JSTAN-234] - Fix for back-to-back multiple OSI21 and IDCO22 LOOKUPTYPE
                  requests.

        Performing multiple LOOKUPTYPE.OSI21 and LOOKUPTYPE.IDCO22 requests
        back-to-back results in only the first request being responded to.

    [JSTAN-235] - Chunked responses from the Tick server results in inaccurate
                  data sets.

01.020.0000:

    Enhancements/Fixes:
    ===================
    [JSTAN-1] - Dynamic Discovery of new Quote fields using a new Quote
                interface methods and Deprecating the old Quote interface
                methods that were using the QUOTEFIELDS enumeration.
    [JSTAN-3] - Support additional fields for the MoneyFund category from
                QuoteManager when using the new Quote interface.
    [JSTAN-4] - Pass CIDs for London and other international exchanges through
                the API when using the new Quote interface.
    [JSTAN-91] - Create migration guide from 1.19.x to 1.20.0.
    [JSTAN-93] - Deprecate the old MarketDepth functionality from the API.

    Details:
    ========

    [JSTAN-1] - Dynamic Discovery of new Quote fields using a new Quote
                interface methods and Deprecating the old Quote interface
                methods that were using the QUOTEFIELDS enumeration.

        Make the Quote interface dynamic such that new data fields can be
        supported without the need to upgrade to a newer version of the API.
        The supported fields by the Quote interface were tied to the QUOTEFIELDS
        enumeration thus any new field required a new release.  The new Quote
        interface removes this limitation by using numeric values instead of a
        predefined enumeration. Also, a new interface Quote.FieldItem has been
        added to read field id, field format and field value for ease of use.

        Response event processing for BundleQuoteRequest and StockOptionRoots
        requests have also been updated to support new Quote interface.

        See the Migration Guide for JStandard 01-020-0000 for full details.
        Below is a summary.

        Deprecated methods in com.esignal.jstandard.beans.quote.Quote:
            Quote(String) constructor
            setSymbol(String)
            putField(QUOTEFIELDS, Object)
            removeField(QUOTEFIELDS)
            getRawfield(QUOTEFIELDS)
            getField(QUOTEFIELDS)
            getFields()
            isFieldNull(QUOTEFIELDS)
            getFieldType(QUOTEFIELDS)
        
        New methods in com.esignal.jstandard.beans.quote.Quote:

            Note: Quote now implements java.lang.Iterable<Quote.FieldItem>
            Iterator<Quote.FieldItem> iterator(). The FieldItem that is returned
            Quote.iterator().next() is only valid during that iteration.
            FieldItem is reused internally by the iterator to reduce object
            creation.

            New interface Quote.FieldItem adds the following methods:
                short getId()
                FIELDFORMAT getFormat()
                <T> T getValue()
                byte getValueAsByte()
                char getValueAsChar()
                short getValueAsShort(boolean readAsUnsigned)
                int getValueAsInteger(boolean readAsUnsigned)
                long getValueAsLong(boolean readAsUnsigned)
                double getValueAsDouble()
                String getValueAsString()
                void getValueAsString(StringBuilder builder)
                BigInteger getValueAsBigInteger(boolean readAsUnsigned)
                CidData getValueAsCidData()
                CidFieldData getValueAsCidFieldData()
                CidPriceFieldData getValueAsCidPriceFieldData()
                byte[] getValueAsByteArray()
                char[] getValueAsCharArray()
                short[] getValueAsShortArray()
                String[] getValueAsStringArray()
            
        Deprecated method in com.esignal.jstandard.event.StockOptionRootsEvent:
            getStockOptionRoots()
    
        New method in com.esignal.jstandard.event.StockOptionRootsEvent:
            getStockOptionRoot()
        
        Deprecated methods in com.esignal.jstandard.managers.MarketDepthManager:
            subscribe(String symbol, MarketDepthListener marketDepthListener,
                MarketDepth.TYPE marketDepthType);
            unsubscribe(String symbol, MarketDepthListener marketDepthListener,
                MarketDepth.TYPE marketDepthType);
        
        New methods in com.esignal.jstandard.managers.MarketDepthManager:
            subscribe(String symbol, MarketDepthListener marketDepthListener,
                MarketDepthRequest marketDepthRequest)
            unsubscribe(String symbol, MarketDepthListener marketDepthListener,
                Long turnAround);

        Deprecated enumerations:
            com.esignal.jstandard.beans.quote.QUOTEFIELDS
            com.esignal.jstandard.beans.quote.CATEGORY
            com.esignal.jstandard.beans.quote.SUBCATEGORY
            com.esignal.jstandard.beans.quote.SYMBOLTYPE
        
            com.esignal.jstandard.beans.marketdepth.MarketDepth.TYPE
        
        New enumerations:
            com.esignal.jstandard.beans.quote.FIELDFORMAT
        
        New classes:
            com.esignal.jstandard.beans.quote.CidData
            com.esignal.jstandard.beans.quote.CidFieldData
            com.esignal.jstandard.beans.quote.CidPriceFieldData

        The samples have been updated to reflect the dynamic Quote interface
        changes.

    [JSTAN-3] - Support additional fields for the MoneyFund category from
                QuoteManager when using the new Quote interface.

    [JSTAN-4] - Pass CIDs for London and other international exchanges through
                the API when using the new Quote interface.

    [JSTAN-91] - Create migration guide from 1.19.x to 1.20.0.

        Refer to the MIGRATION section for 01.020.0000.

    [JSTAN-93] - Deprecate the old MarketDepth functionality from the API.
