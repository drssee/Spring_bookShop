package com.example.orders.controller;

import com.example.book.service.BookService;
import com.example.exception.IllegalUserException;
import com.example.orders.controller.dto.OrdersBookDto;
import com.example.orders.controller.form.OrdersForm;
import com.example.orders.service.OrdersService;
import com.example.orders.vo.OrdersBookVO;
import com.example.orders.vo.OrdersVO;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.common.UtilMethod.getUser;
import static com.example.common.status.OrderStatus.DELIVERY_COMPLETE;
import static com.example.common.status.OrderStatus.PAYMENT_COMPLETE;
import static com.example.common.status.RequestStatus.INVALID_USER;
import static com.example.common.status.RequestStatus.UNAUTHORIZED;
import static com.example.common.validator.BookshopValidator.validateAdmin;
import static com.example.common.validator.BookshopValidator.validateLoginedUser;

@Controller
@RequestMapping("/orders")
@Slf4j
public class OrdersController {

    private final OrdersService ordersService;
    private final BookService bookService;

    private final IamportClient api;
    public OrdersController(OrdersService ordersService, BookService bookService) {
        this.ordersService = ordersService;
        this.bookService = bookService;
        // REST API ?????? REST API secret ??? ???????????? ???????????? ????????????.
        this.api = new IamportClient("5006845628855560","BrkGFTinWQ1zKsrKaUEWnLnnDXX5xm850hlZWSGW5AbY9szOFuBA0FmCJJu9pBei47Hc2G4J58cW54u0");
    }

    // *OrdersController api*

    // post /orders/payment <-????????????, ??????
    // post /orders/verifyIamport/{imp_uid} <-iamport ?????? api
    // post /orders/deleteIamport/{imp_uid} <-iamport ?????? api
    // get /orders/result <-???????????? ?????????
    // get /orders/list <-???????????? ?????????
    // get /orders/list/{id} <-???????????? ?????????(????????? ??????)
    // post /orders/delevery/{orders_id} <-??????(????????? ??????)

    /**
     * ????????????
     */
    @PostMapping("/payment")
    @ResponseBody
    public ResponseEntity<String> payment(@Validated @RequestBody OrdersForm ordersForm
            , BindingResult bindingResult, HttpServletRequest request){
        /*
        valid
        */
        //orderForm ??????
        if(bindingResult.hasErrors()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        //????????? ??????
        if(!validateLoginedUser(ordersForm.getId(),request)){
            throw new IllegalUserException("??????id???, ?????????id??? ???????????? ????????????.");
        }

        /*
        core
        */
        //OrdersVO?????????
        OrdersVO ordersVO = new OrdersVO(ordersForm);
        //OrdersBookVOList ?????????
        List<OrdersBookVO> ordersBookVOList = new ArrayList<>();
        ordersForm.getOrdersBookForms().forEach(ordersBookForm -> {
            OrdersBookVO ordersBookVO = new OrdersBookVO(ordersBookForm);
            ordersBookVOList.add(ordersBookVO);
        });
        //?????? ??????
        ordersService.buyBook(ordersVO,ordersBookVOList,ordersForm.isFromCart());
        return ResponseEntity.ok("payment success");
    }


    /**
     * iamport ?????? api
     */
    @PostMapping("/verifyIamport/{imp_uid}")
    @ResponseBody
    public IamportResponse<Payment> paymentByImpUid(
            Model model
            , Locale locale
            , HttpSession session
            , @PathVariable(value= "imp_uid") String imp_uid) throws IamportResponseException, IOException
    {
        return api.paymentByImpUid(imp_uid);
    }

    /**
     * iamport ?????? ??????
     */
    @PostMapping("/deleteIamport/{imp_uid}")
    @ResponseBody
    public ResponseEntity<Boolean> deleteOrders(@PathVariable String imp_uid, @Param("order_id") Long order_id,
                                                HttpServletRequest request){

        //imp_uid ??????
        if(imp_uid==null||"".equals(imp_uid)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "???????????? ????????????");
        }
        //order_id ?????? ???????????????
        if(order_id!=null){
            //orderStatus ??????, ???????????? ????????? ????????? ?????????(???????????????)
            if(!PAYMENT_COMPLETE.label()
                    .equals(ordersService.getOrders(order_id).getOrder_status())){
                //??????????????? ??????(???????????? ????????? throw exception)
                if(validateAdmin(request)){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"???????????? ?????????");
                }
            }
            //orders,orders_book db?????? ??????
            ordersService.cancelOrders(order_id);
        }

        //iamport ????????????
        try {
            //????????????(imp_uid)??? ??????????????? ????????? canceldata??? ?????????
            CancelData cancelData = new CancelData(imp_uid,true);
            //iamport ???????????????
            api.cancelPaymentByImpUid(cancelData);
        } catch (IamportResponseException | IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"?????? ????????? ??????????????????.");
        }

        return ResponseEntity.ok(true);
    }

    /**
     * ???????????? ?????????
     */
    @GetMapping("/result")
    public String result(){
        return "orders/result";
    }

    /**
     * ???????????? ?????????
     */
    @GetMapping("/list")
    public String list(HttpServletRequest request, Model model){
        /*
        valid
        */
        //????????? ??????
        if(!validateLoginedUser(request)){
            throw new IllegalUserException(INVALID_USER.label());
        }

        /*
        core
        */
        //ordersBooks ?????????
        List<OrdersBookDto> ordersBookDtos = ordersService.getOrdersBookDtos(getUser(request).getId());
        //ordersBookVOList?????? bno????????? bookVO ?????????
        ordersBookDtos.forEach(ordersBookDto -> {
            ordersBookDto.getOrdersBookVOList().forEach(ordersBookVO -> {
                ordersBookVO.setBookVO(bookService.getBook(ordersBookVO.getBno()));
            });//?????? foreach
        });//?????? foreach

        //???????????? ordersBooks ????????? ??????
        model.addAttribute("ordersBooks",ordersBookDtos);
        return "orders/orders";
    }

    /**
     * ???????????? ?????????
     */
    @GetMapping("/list/{id}")
    public String list(@PathVariable String id, HttpServletRequest request, Model model){
        /*
        valid
        */
        //????????? ??????
        if(validateAdmin(request)){
            throw new IllegalUserException(UNAUTHORIZED.label());
        }

        /*
        core
        */
        //ordersBooks ?????????
        List<OrdersBookDto> ordersBookDtos = ordersService.getOrdersBookDtos(id);
        //ordersBookVOList?????? bno????????? bookVO ?????????
        ordersBookDtos.forEach(ordersBookDto -> {
            ordersBookDto.getOrdersBookVOList().forEach(ordersBookVO -> {
                ordersBookVO.setBookVO(bookService.getBook(ordersBookVO.getBno()));
            });//?????? foreach
        });//?????? foreach

        //???????????? ordersBooks ????????? ??????
        model.addAttribute("ordersBooks",ordersBookDtos);
        return "orders/orders";
    }

    /**
     * ??????(???????????????)
     */
    @PostMapping("/delevery/{order_id}")
    @ResponseBody
    public ResponseEntity<Boolean> delevery(@PathVariable Long order_id, HttpServletRequest request){
        /*
        valid
        */
        //????????? ??????
        if(validateAdmin(request)){
            throw new IllegalUserException(UNAUTHORIZED.label());
        }

        /*
        core
        */
        //???????????? ??????
        OrdersVO orders = ordersService.getOrders(order_id);
        orders.setOrder_status(DELIVERY_COMPLETE.label());
        return ResponseEntity.ok(ordersService.updateOrders(orders)==1);
    }
}
