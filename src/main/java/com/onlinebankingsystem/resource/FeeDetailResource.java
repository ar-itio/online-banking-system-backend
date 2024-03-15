package com.onlinebankingsystem.resource;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.onlinebankingsystem.dto.CommonApiResponse;
import com.onlinebankingsystem.dto.FeeDetailResponse;
import com.onlinebankingsystem.entity.FeeDetail;
import com.onlinebankingsystem.service.FeeDetailService;
import com.onlinebankingsystem.utility.Constants.FeeType;

@Component
public class FeeDetailResource {

    private final Logger LOG = LoggerFactory.getLogger(FeeDetailResource.class);

    @Autowired
    private FeeDetailService feeDetailService;

    public ResponseEntity<CommonApiResponse> addFeeDetail(FeeDetail feeDetail) {

        LOG.info("Received request for register user");

        CommonApiResponse response = new CommonApiResponse();

        if (feeDetail == null || feeDetail.getFee() == null || feeDetail.getType() == null) {
            response.setResponseMessage("bad request - missing data");
            response.setSuccess(true);

            return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
        }

        FeeDetail fetchedFeeDetail = this.feeDetailService.getFeeDetailByType(feeDetail.getType());

        if (fetchedFeeDetail == null) {
            this.feeDetailService.addFeeDetail(feeDetail);
        } else {
            fetchedFeeDetail.setFee(feeDetail.getFee());
            this.feeDetailService.addFeeDetail(fetchedFeeDetail);
        }

        response.setResponseMessage("Fee Detail Updated Successful!!!");
        response.setSuccess(true);

        return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);

    }

    public ResponseEntity<FeeDetailResponse> fetchFeeDetails() {

        LOG.info("Received request for fetching fee details");

        FeeDetailResponse response = new FeeDetailResponse();

        List<FeeDetail> feeDetails = this.feeDetailService.getAllFeeDetails();

        if (feeDetails == null) {
            response.setResponseMessage("no fees entry found");
            response.setSuccess(false);

            return new ResponseEntity<FeeDetailResponse>(response, HttpStatus.OK);
        }

        response.setFeeDetails(feeDetails);
        response.setResponseMessage("Fee Detail Fetched Successful!!!");
        response.setSuccess(true);

        return new ResponseEntity<FeeDetailResponse>(response, HttpStatus.OK);

    }

    public ResponseEntity fetchFeeType() {

        List<String> feeTypes = new ArrayList<>();

        for (FeeType type : FeeType.values()) {
            feeTypes.add(type.value());
        }

        return new ResponseEntity(feeTypes, HttpStatus.OK);
    }

}