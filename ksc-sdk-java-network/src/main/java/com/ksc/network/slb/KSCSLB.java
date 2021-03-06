package com.ksc.network.slb;

import com.ksc.network.slb.model.DescribeLoadBalancersRequest;
import com.ksc.network.slb.model.DescribeLoadBalancersResult;

public interface KSCSLB {
	/**
	 * <p>
	 * Describes one or more of your LoadBalancers.
	 * </p>
	 * 
	 * @param describeLoadBalancersRequest
	 *            Contains the parameters for DescribeLoadBalancers.
	 * @return Result of the DescribeAddresses operation returned by the
	 *         service.
	 * @sample KSC.DescribeLoadBalancers
	 */
	public DescribeLoadBalancersResult describeLoadBalancers(DescribeLoadBalancersRequest describeLoadBalancersRequest);
}
