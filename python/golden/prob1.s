port:
    word 1
max_palindrome:
    word 0
start:
    lit 999             ; i
outer_loop:
    dup                 ; i i
    lit 100             ; i i 100
    sub                 ; i i-100
    lit end             ; i i-100 end
    swap                ; i end i-100
    jn                  ; i
    lit 999             ; i j
inner_loop:
    dup                 ; i j j
    lit 100             ; i j j 100
    sub                 ; i j j-100
    lit end_inner       ; i j j-100 end_inner
    swap                ; i j end_inner j-100
    jn                  ; i j
    swap                ; j i
    over                ; i j i
    swap                ; i i j
    over                ; i j i j
    mul                 ; i j i*j
    dup                 ; i j i*j i*j
    lit is_palindrome   ; i j i*j i*j is_palindrome
    call                ; i j i*j is_palindrome_res
    lit next_iter       ; i j i*j is_palindrome_res next_iter
    swap                ; i j i*j next_iter is_palindrome_res
    jz                  ; i j i*j
    dup                 ; i j i*j
    dup                 ; i j i*j
    lit max_palindrome  ; i j i*j i*j max_palindrome_addr
    load                ; i j i*j i*j max_palindrome
    swap                ; i j i*j max_palindrome i*j
    sub                 ; i j i*j i*j-max_palindrome
    lit next_iter       ; i j i*j i*j-max_palindrome next_iter
    swap                ; i j i*j next_iter i*j-max_palindrome
    jn                  ; i j i*j
    lit max_palindrome  ; i j i*j max_palindrome_addr
    store               ; i j
    dup                 ; i j j
next_iter:
    drop                ; i j
    dec                 ; i j-1
    lit inner_loop      ; i j-1 inner_loop
    jump                ; i j-1
end_inner:
    drop                ; i
    dec                 ; i-1
    lit outer_loop      ; i-1 outer_loop
    jump
end:
    drop                ;
    lit max_palindrome  ; max_palindrome_addr
    load                ; max_palindrome
    lit print_number    ; max_palindrome print_number
    call                ;
    halt
    is_palindrome:
    dup                 ; arg arg
    lit 0               ; arg arg rev
    swap                ; arg rev arg
reverse_loop:
    dup                 ; arg rev arg arg
    lit check_equal     ; arg rev arg arg check_equal
    swap                ; arg rev arg check_equal arg
    jn                  ; arg rev arg
    dup                 ; arg rev arg arg
    lit 10              ; arg rev arg arg 10
    swap                ; arg rev arg 10 arg
    mod                 ; arg rev arg digit
    over                ; arg rev digit arg
    drop                ; arg rev digit
    swap                ; arg digit rev
    lit 10              ; arg digit rev 10
    mul                 ; arg digit rev*10
    add                 ; arg rev*10+digit
    swap                ; rev*10+digit arg
    over                ; arg rev*10+digit arg
    lit 10              ; arg rev*10+digit arg 10
    over                ; arg rev*10+digit 10 arg 10 
    lit 10              ; arg rev*10+digit arg 10
    over                ; arg rev*10+digit 10 arg 10
    drop                ; arg rev*10+digit 10 arg
    div                 ; arg rev*10+digit arg/10
    lit reverse_loop    ; arg rev*10+digit arg/10 reverse_loop
    jump                ; arg rev*10+digit arg/10
check_equal:
    drop                ; arg rev
    sub                 ; rev-arg
    lit true_palindrome ; rev-arg true_palindrome
    swap                ; true_palindrome rev-arg
    jz                  ;
    lit 0               ; false
    ret
true_palindrome:
    lit 1               ; true
    ret
    print_digit:
    lit 48              ; arg 48
    add                 ; arg -> char
    lit port            ; char port_addr
    load                ; char port
    out                 ;
    ret                 ;
    print_number:
    lit -1              ; num -1
    swap                ; -1 num
    dup                 ; -1 num num
    lit negate          ; -1 num num negate
    swap                ; -1 num negate num
    jn                  ; -1 num
    lit loop1           ; -1 num loop1
    jump                ; -1 num
negate:
    lit 45              ; [...] 45
    lit port            ; [...] 45 port_a
    load                ; [...] 45 port
    out                 ; [...]
    lit -1              ; 0 num -1
    mul                 ; 0 -num -> num
loop1:
    dup                 ; 0 num num
    lit 10              ; 0 num num 10
    mod                 ; 0 num digit
    swap                ; 0 digit num
    lit 10              ; 0 digit num 10
    div                 ; 0 digit num/10 -> num
    over                ; 0 num digit num
    lit loop1exit       ; [...] num loop2
    swap                ; [...] loop2 num
    jz                  ; [...] num digit
    swap
    lit loop1           ; [...] num loop1
    jump                ; [...] num
loop1exit:
    swap                ; [...] 0 digit
    drop                ; [...] digit
loop2:
    dup                 ; digit digit
    lit break           ; digit digit break
    swap                ; digit break digit
    jn                  ; digit
    lit print_digit     ; digit print_digit
    call                ; digit
    lit loop2           ; loop2
    jump                ;
break:
    ret                 ;