/**
 * Copyright (C) 2011 - 2013 Alfresco Business Reporting project
 *
 * This file is part of the Alfresco Business Reporting project.
 *
 * Licensed under the GNU LGPL, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.gnu.org/licenses/lgpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.alfresco.reporting.cron;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.atomic.AtomicBoolean;

import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.lock.JobLockService.JobLockRefreshCallback;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.reporting.action.executer.ReportRootExecuter;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.InitializingBean;

public class ReportExecutionJob implements Job, InitializingBean {

	protected JobLockService jobLockService;
	protected ActionService actionService;
	private int startDelayMinutes=0;
	
	private static Log logger = LogFactory.getLog(ReportExecutionJob.class);

	// Lock key
    private QName lock = QName.createQName(ReportingModel.REPORTING_URI, "ReportExecutionJob");;

    public ActionService getActionService() {
		return actionService;
	}

    public void setStartDelayMinutes(int startDelayMinutes) {
		this.startDelayMinutes = startDelayMinutes;
	}
    
	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}
    
	private QName getLockKey(){
    	logger.debug("Returning lock " + lock.toString());
    	return lock;
    }
    
	 private String getLock(QName lock, long time)
	    {
	        try
	        {
	            return jobLockService.getLock(lock, time);
	        }
	        catch (LockAcquisitionException e)
	        {
	            return null;
	        }
	    }

	    /* (non-Javadoc)
	     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	     */
	    @Override
	    public void afterPropertiesSet() throws Exception
	    {
	       lock  = QName.createQName(ReportingModel.REPORTING_URI, "ReportExecutionJob");
	        
	    }

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap jobData = context.getJobDetail().getJobDataMap();
		this.jobLockService   = (JobLockService)jobData.get("jobLockService");
		
		logger.debug("jobLockService hashcode=" +jobLockService.hashCode());
		
		// get a token for a minute
        String lockToken = getLock(getLockKey(), 60000);
        if (lockToken == null)
        {
            return;
        }
        // Use a flag to keep track of the running job
        final AtomicBoolean running = new AtomicBoolean(true);
        jobLockService.refreshLock(lockToken, lock, 30000, new JobLockRefreshCallback()
        {
            @Override
            public boolean isActive()
            {
                return running.get();
            }

            @Override
            public void lockReleased()
            {
                running.set(false);
            }
        });
        try
        {
        	logger.debug("Start executeImpl");
            executeImpl(running, context);
            logger.debug("End executeImpl");
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        finally
        {
            // The lock will self-release if answer isActive in the negative
            running.set(false);
            jobLockService.releaseLock(lockToken, getLockKey());
            logger.debug("Released the lock");
        }
	}

	public void executeImpl(AtomicBoolean running, JobExecutionContext context) throws JobExecutionException {
		
		// actually do what needs to be done
		// a cron job can only trigger 
		//   -all- ReportingContainers with same frequency
		//   -all- ReprotingContainers no matter what frequency (all-all) 
		final JobDataMap jobData = context.getJobDetail().getJobDataMap();
		
		// can be null, then execute all frequencies
		final String frequency = jobData.getString("frequency"); 
		
		if (logger.isDebugEnabled()) 
			logger.debug("executeImpl starting with frequency " + frequency);
		
		int startupDelayMillis=0;
		startupDelayMillis = Integer.parseInt(jobData.getString("startDelayMinutes"))*60000;
		
		if (logger.isDebugEnabled()) 
			logger.debug("executeImpl starting with delay " + startupDelayMillis);
		
		// wait system i starting
		RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
		if (startupDelayMillis < rb.getUptime()){
			// we are x minutes after startup of the repository, lets roll!
				AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Void>() {
				    public Void doWork() {
				    	Action action = actionService
								.createAction(ReportRootExecuter.NAME);
				    	
				    	boolean canRun=false; // make true if run can be successful...
						if (frequency!=null){
							
							if ("hourly".equals(frequency)){
								action.setParameterValue(ReportRootExecuter.PARAM_FREQUENCY,
										frequency);
								canRun=true;
							} // end hourly
							
							if ("daily".equals(frequency)){
								action.setParameterValue(ReportRootExecuter.PARAM_FREQUENCY,
										frequency);
								canRun=true;
							} // end daily
							
							if ("weekly".equals(frequency)){
								action.setParameterValue(ReportRootExecuter.PARAM_FREQUENCY,
										frequency);
								canRun=true;
							} // end weekly
							
							if ("monthly".equals(frequency)){
								action.setParameterValue(ReportRootExecuter.PARAM_FREQUENCY,
										frequency);
								canRun=true;
							} // end monthly
							
							if (canRun) {
								if (logger.isDebugEnabled())
									logger.debug("executeImpl: start running action " + frequency);
								actionService.executeAction(action, null);
							} else {
								logger.error("executeImpl: Confguration mistake, cannot run. Frequency has no meaningful value: " + frequency);
							}
							
						} // end if frequency!=null
						else {
							
							action.setParameterValue(ReportRootExecuter.PARAM_FREQUENCY, "hourly");
							actionService.executeAction(action, null);
							
							action.setParameterValue(ReportRootExecuter.PARAM_FREQUENCY, "daily");
							actionService.executeAction(action, null);
		
							action.setParameterValue(ReportRootExecuter.PARAM_FREQUENCY, "weekly");
							actionService.executeAction(action, null);
							
							action.setParameterValue(ReportRootExecuter.PARAM_FREQUENCY, "monthly");
							actionService.executeAction(action, null);
							
						}
						
				    	return null;
				    } // end doWork
				}, AuthenticationUtil.getSystemUserName());
		} else {
			logger.info("Hey relax! Its too early to work... Let the repository get up first... Aborting this run.");
		}
	}
}
