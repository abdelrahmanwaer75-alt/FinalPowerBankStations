package com.powerbank.dto;

import java.util.List;

public record AdminAccountRequestsResponse(boolean canApprove, List<AdminAccountRequestRow> requests) { }
